/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ldtteam.teamcity.github;

import com.google.common.collect.ImmutableList;
import com.jcabi.github.*;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildLog.BlockLogMessage;
import jetbrains.buildServer.serverSide.buildLog.MessageAttrs;
import jetbrains.buildServer.serverSide.db.SQLRunnerEx;
import jetbrains.buildServer.serverSide.impl.codeInspection.InspectionInfo;
import jetbrains.buildServer.vcs.FilteredVcsChange;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsChange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GithubCommentingBuildServerAdapter extends BuildServerAdapter
{

    private final SBuildServer server;

    public GithubCommentingBuildServerAdapter(SBuildServer sBuildServer)
    {
        server = sBuildServer;
    }

    public void register()
    {
        server.addListener(this);
    }

    @Override
    public void beforeBuildFinish(@NotNull final SRunningBuild runningBuild)
    {
        final Optional<SBuildFeatureDescriptor> commentingBuildFeature = runningBuild.getBuildFeaturesOfType(GithubCommentingBuildFeature.class.getName()).stream().findFirst();

        if (commentingBuildFeature.isPresent())
        {
            final InspectionInfo info = getInspectionInfo(runningBuild);
            final List<String[]> inspectionData = info.getInspections();
            final Map<Long, String> inspectionIdsWithName =
              inspectionData.stream().filter(data -> Integer.parseInt(data[6]) >= 0).collect(Collectors.toMap(data -> Long.parseLong(data[2]), data -> data[3]));

            final List<FilteredVcsChange> changedFiles = runningBuild
                                                           .getChanges(SelectPrevBuildPolicy.SINCE_LAST_COMPLETE_BUILD, false)
                                                           .stream()
                                                           .flatMap(vcs -> vcs.getFilteredChanges(runningBuild).stream())
                                                           .collect(Collectors.toList());

            final Map<String, Map<Long, List<String[]>>> fileData = changedFiles
                                                                      .stream()
                                                                      .collect(
                                                                        Collectors.toMap(
                                                                          VcsChange::getRelativeFileName,
                                                                          filteredVcsChange -> inspectionIdsWithName
                                                                                                 .keySet()
                                                                                                 .stream()
                                                                                                 .collect(
                                                                                                   Collectors.toMap(
                                                                                                     Function.identity(),
                                                                                                     //Line - Inspection info - R Count - Severity
                                                                                                     aLong -> info.getDetails(aLong, filteredVcsChange.getFileName(), false)
                                                                                                   )
                                                                                                 )
                                                                        )
                                                                      );


            final BlockLogMessage openGithubCommentingBlock = runningBuild.getBuildLog().openBlock("Github PR Commenting", getClass().getName(), MessageAttrs.serverMessage());

            runningBuild.getBuildLog().message("Discovered Inspections:", Status.NORMAL, MessageAttrs.serverMessage());
            fileData
              .keySet()
              .stream()
              .forEach(fileName-> {
                  final BlockLogMessage openFileBlockMessage = runningBuild.getBuildLog().openBlock(fileName, getClass().getName() + "_file", MessageAttrs.serverMessage());

                  fileData
                    .get(fileName)
                    .keySet()
                    .stream()
                    .filter(inspectionId -> !fileData.get(fileName).get(inspectionId).isEmpty())
                    .forEach(inspectionId -> {
                        final BlockLogMessage openFileInspectionBlockMessage = runningBuild.getBuildLog().openBlock(inspectionIdsWithName.get(inspectionId), getClass().getName() + "_file_inspection", MessageAttrs.serverMessage());

                        fileData
                          .get(fileName)
                          .get(inspectionId)
                          .stream()
                          .forEach(inspectionFileData -> {
                              runningBuild.getBuildLog().message(inspectionFileData[1] + " On line: " + inspectionFileData[0] + " with severity: " + inspectionFileData[3], Integer.parseInt(inspectionFileData[3]) < 3 ? Status.WARNING :
                                Status.NORMAL, MessageAttrs.serverMessage());
                          });

                        runningBuild.getBuildLog().closeBlock(inspectionIdsWithName.get(inspectionId), getClass().getName() + "_file_inspection", Date.from(Instant.now()), String.valueOf(openFileInspectionBlockMessage.getFlowId()));
                    });

                  runningBuild.getBuildLog().closeBlock(fileName, getClass().getName() + "_file", Date.from(Instant.now()), String.valueOf(openFileBlockMessage.getFlowId()));
              });

            runningBuild.getBuildLog().message("Starting Github upload.", Status.NORMAL, MessageAttrs.serverMessage());

            final SBuildFeatureDescriptor featureDescriptor = commentingBuildFeature.get();
            final Map<String, String> parameters = featureDescriptor.getParameters();

            final String token = parameters.get("token");
            final Integer pullId = Integer.parseInt(parameters.get("branch"));

            try
            {
                final Github gh = new RtGithub(token);

                final String url = runningBuild.getVcsRootEntries().get(0).getProperties().get("url");
                final String[] urlComponents = url.split("/");

                final String ownerName = urlComponents[urlComponents.length - 2];
                final String repoName = urlComponents[urlComponents.length - 1].replace(".git", "");

                final Repo repo = gh.repos().get(new Coordinates.Simple(ownerName, repoName));
                final Pull pullRequest = repo.pulls().get(pullId);
                pullRequest.comments()
            }
            catch (Exception e)
            {
                runningBuild.getBuildLog().error("FAILURE", e.getLocalizedMessage(), Date.from(Instant.now()), e.getLocalizedMessage(), String.valueOf(openGithubCommentingBlock.getFlowId()),
                  ImmutableList.of());
            }
        }
        else
        {
            System.out.println("No PR commenting detected.");
        }
    }

    @Nullable
    private InspectionInfo getInspectionInfo(SBuild build)
    {
        BuildTypeEx buildType = (BuildTypeEx) build.getBuildType();
        return buildType == null ? null : new InspectionInfo((SQLRunnerEx) server.getSQLRunner(), build);
    }
}
