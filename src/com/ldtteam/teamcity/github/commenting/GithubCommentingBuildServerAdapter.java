package com.ldtteam.teamcity.github.commenting;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.ldtteam.teamcity.github.inspections.FileInspectionData;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildLog.BlockLogMessage;
import jetbrains.buildServer.serverSide.buildLog.MessageAttrs;
import jetbrains.buildServer.serverSide.db.SQLRunnerEx;
import jetbrains.buildServer.serverSide.impl.codeInspection.InspectionInfo;
import jetbrains.buildServer.vcs.FilteredVcsChange;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.heading.Heading;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
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
        processFinishedBuild(runningBuild);
    }

    private void processFinishedBuild(@NotNull final SRunningBuild runningBuild)
    {
        final Optional<SBuildFeatureDescriptor> commentingBuildFeature = runningBuild.getBuildFeaturesOfType(GithubCommentingBuildFeature.class.getName()).stream().findFirst();

        if (!commentingBuildFeature.isPresent())
        {
            return;
        }

        final BlockLogMessage openGithubCommentingBlock = runningBuild.getBuildLog().openBlock("Github PR Commenting", getClass().getName(), MessageAttrs.serverMessage());

        final SBuildFeatureDescriptor featureDescriptor = commentingBuildFeature.get();
        final Map<String, String> parameters = featureDescriptor.getParameters();

        final String username = parameters.get("username");
        final String token = parameters.get("token");
        final String password = parameters.get("password");
        final String url = runningBuild.getVcsRootEntries().get(0).getProperties().get("url");
        final String[] urlParts = url.split("/");
        final String repoName = urlParts[urlParts.length - 2] + "/" + urlParts[urlParts.length - 1].replace(".git", "");

        try
        {
            final Integer pullId = Integer.parseInt(parameters.get("branch"));

            try
            {

                final GitHub github = new GitHubBuilder().withOAuthToken(token, username).withPassword(username, password).build();

                if (!github.isCredentialValid())
                {
                    throw new IllegalAccessException("Could not authenticate configured user against GitHub.");
                }

                final GHRepository repo = github.getRepository(repoName);
                final GHPullRequest request = repo.getPullRequest(pullId);

                dismissLastPullRequestReview(request, username);

                final GHPullRequestReviewBuilder builder = github.getRepository(repoName).getPullRequest(pullId).createReview();

                final List<SBuild> activeBuilds =
                  runningBuild
                    .getBuildPromotion()
                    .getAllDependencies()
                    .stream()
                    .filter(p -> p.getAssociatedBuild() != null)
                    .map(BuildPromotion::getAssociatedBuild).collect(Collectors.toList());
                activeBuilds.add(runningBuild);

                final List<InspectionInfo> activeInfos =
                  activeBuilds
                    .stream()
                    .map(this::getInspectionInfo)
                    .collect(Collectors.toList());

                final List<List<String[]>> inspectionIds =
                  activeInfos
                    .stream()
                    .map(InspectionInfo::getInspections)
                    .collect(Collectors.toList());

                final Map<Long, String> idNameMap =
                  inspectionIds
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(data -> Integer.parseInt(data[6]) > 0)
                    .distinct()
                    .collect(Collectors.toMap(
                      data -> Long.parseLong(data[2]),
                      data -> data[3],
                      (nameOne, nameTwo) -> nameOne
                    ));

                final Collection<String> changedFiles = getChangesForBuild(runningBuild, activeBuilds);

                changedFiles.stream().forEach(file -> {
                    final FileInspectionData fileInspectionData = new FileInspectionData(file, activeInfos, idNameMap.keySet(), request);
                    fileInspectionData.process(builder);
                });

                final int[] statisticsTotal = getStatistics(activeInfos);
                final int diffErrors = statisticsTotal[4] - statisticsTotal[5];

                final StringBuilder bodyBuilder = new StringBuilder()
                                                    .append(new Heading("Analysis Complete", 4)).append("\n")
                                                    .append(new Heading("Statistics", 5)).append("\n")
                                                    .append(createStatisticContent(statisticsTotal).build());

                builder.body(bodyBuilder.toString());

                builder.event(diffErrors < 0 ? GHPullRequestReviewEvent.REQUEST_CHANGES : GHPullRequestReviewEvent.APPROVE);
                builder.create();
            }
            catch (Exception e)
            {
                runningBuild.getBuildLog()
                  .error("FAILURE", e.getMessage(), Date.from(Instant.now()), e.getLocalizedMessage(), String.valueOf(openGithubCommentingBlock.getFlowId()),
                    ImmutableList.of());
            }
        }
        catch (NumberFormatException nfe)
        {
            runningBuild.getBuildLog().message("Cannot upload comments to Github. Branch is not a number.", Status.ERROR, MessageAttrs.serverMessage());
        }

        runningBuild.getBuildLog().closeBlock("Github PR Commenting", getClass().getName(), Date.from(Instant.now()), String.valueOf(openGithubCommentingBlock.getFlowId()));
    }

    private void dismissLastPullRequestReview(final GHPullRequest request, final String actingUserName)
    {
        request.listReviews().asList().stream().filter(r -> !r.getState().equals(GHPullRequestReviewState.DISMISSED)).filter(r -> {
            try
            {
                return r.getUser().getName().equals(actingUserName);
            }
            catch (IOException e)
            {
                //Noop- We do not care, if we can not get the user we can not dismiss it either.
            }

            return false;
        }).forEach(r -> {
            try
            {
                r.dismiss("A new analysis is being ran. Please wait for the results.");
            }
            catch (IOException e)
            {
                //Noop - We do not care, if we can not dismiss the review.
            }
        });
    }

    private Collection<String> getChangesForBuild(final SBuild targetBuild, final Collection<SBuild> buildGraph)
    {
        final List<SVcsModification> currentBuildChanges = targetBuild.getChanges(SelectPrevBuildPolicy.SINCE_NULL_BUILD, true);

        if (targetBuild.getBuildPromotion().getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_COMPLETE_BUILD) == null)
        {
            return getFilteredChangesForBuilds(currentBuildChanges, buildGraph);
        }

        final List<SVcsModification> previousBuildChanges =
          targetBuild.getBuildPromotion().getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_COMPLETE_BUILD).getChanges(SelectPrevBuildPolicy.SINCE_NULL_BUILD, true);


        return getFilteredChangesForBuilds(currentBuildChanges
                 .stream()
                 .filter(c -> !previousBuildChanges.contains(c))
                 .collect(Collectors.toList()), buildGraph);
    }

    private Collection<String> getFilteredChangesForBuilds(final Collection<SVcsModification> modifications, final Collection<SBuild> builds)
    {
        return modifications
          .stream()
          .flatMap(
            sVcsModification -> {
                final Collection<FilteredVcsChange> change = Lists.newArrayList();
                builds.forEach(build -> {
                    change.addAll(sVcsModification.getFilteredChanges(build));
                });
                return change.stream();
            }
          )
                 .map(FilteredVcsChange::getRelativeFileName)
          .distinct()
          .collect(Collectors.toList());
    }

    @Nullable
    private InspectionInfo getInspectionInfo(SBuild build)
    {
        BuildTypeEx buildType = (BuildTypeEx) build.getBuildType();
        return buildType == null ? null : new InspectionInfo((SQLRunnerEx) server.getSQLRunner(), build);
    }

    private final Table.Builder createStatisticContent(final int[] statisticsTotal)
    {
        final int diffTotal = statisticsTotal[1] - statisticsTotal[2];
        final int diffErrors = statisticsTotal[4] - statisticsTotal[5];

        final Table.Builder builder = new Table.Builder()
                                        .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_CENTER, Table.ALIGN_CENTER)
                                        .addRow("", "Count", "New", "Old", "Diff")
                                        .addRow("Total", statisticsTotal[0], statisticsTotal[1], statisticsTotal[2], String.format("%s%d", diffTotal > 0 ? "+" : "", diffTotal))
                                        .addRow("Errors", statisticsTotal[3], statisticsTotal[4], statisticsTotal[5], String.format("%s%d", diffTotal > 0 ? "+" : "", diffErrors));

        return builder;
    }

    private int[] getStatistics(final Collection<InspectionInfo> inspectionInfos)
    {
        final Collection<int[]> statisticsPerInfo = inspectionInfos
                                                      .stream()
                                                      .map(InspectionInfo::getStatistics)
                                                      .collect(Collectors.toList());

        return statisticsPerInfo
                                        .stream()
                                        .reduce(
                                          new int[6],
                                          (one, two) -> {
                                              if (one == null)
                                                  return two == null ? new int[6] : two;

                                              if (two == null)
                                                  return one;

                                              return new int[] {one[0] + two[0], one[1] + two[1], one[2] + two[2], one[3] + two[3], one[4] + two[4], one[5] + two[5]};
                                          }
                                        );
    }
}
