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

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.db.SQLRunnerEx;
import jetbrains.buildServer.serverSide.impl.codeInspection.InspectionInfo;
import jetbrains.buildServer.vcs.FilteredVcsChange;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsChange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        System.out.println("Build finishing.");

        if (!runningBuild.getBuildFeaturesOfType(GithubCommentingBuildFeature.class.getName()).isEmpty())
        {
            System.out.println("Detected PR Commenting.");
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
                                                                                                     aLong -> info.getDetails(aLong, filteredVcsChange.getFileName(), false)
                                                                                                   )
                                                                                                 )
                                                                        )
                                                                      );

            System.out.println("################################");
            fileData
              .keySet()
              .stream()
              .forEach(fileName-> {
                  System.out.println(" - File: " + fileName);
                  fileData
                    .get(fileName)
                    .keySet()
                    .stream()
                    .forEach(inspectionId -> {
                        System.out.println("  ~ Inspection: " + inspectionIdsWithName.get(inspectionId));

                        fileData
                          .get(fileName)
                          .get(inspectionId)
                          .stream()
                          .forEach(inspectionFileData -> {
                              System.out.println("   > " + Arrays.toString(inspectionFileData));
                          });
                    });
              });
            System.out.println("################################");
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
