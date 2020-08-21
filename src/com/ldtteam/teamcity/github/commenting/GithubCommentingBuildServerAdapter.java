package com.ldtteam.teamcity.github.commenting;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.ldtteam.teamcity.github.inspections.FileInspectionData;
import com.ldtteam.teamcity.github.utils.RSA;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildLog.BlockLogMessage;
import jetbrains.buildServer.serverSide.buildLog.MessageAttrs;
import jetbrains.buildServer.serverSide.db.SQLRunnerEx;
import jetbrains.buildServer.serverSide.impl.codeInspection.InspectionInfo;
import jetbrains.buildServer.vcs.FilteredVcsChange;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import net.steppschuh.markdowngenerator.link.Link;
import net.steppschuh.markdowngenerator.list.UnorderedList;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.heading.Heading;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.*;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.stream.Collectors;

public class GithubCommentingBuildServerAdapter extends BuildServerAdapter
{

    private static final String INSPECTION_URL_PATTERN = "https://teamcity.minecolonies.com/viewLog.html?buildId=%d&tab=Inspection&buildTypeId=%s";

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
    public void buildStarted(@NotNull final SRunningBuild build)
    {
        final Optional<SBuildFeatureDescriptor> commentingBuildFeature = build.getBuildFeaturesOfType(GithubCommentingBuildFeature.class.getName()).stream().findFirst();

        if (!commentingBuildFeature.isPresent())
        {
            return;
        }

        final BlockLogMessage openGithubCommentingBlock =
          build.getBuildLog().openBlock("Github PR Commenting - Dismissing existing review", getClass().getName() + "-Dismiss", MessageAttrs.serverMessage());

        final SBuildFeatureDescriptor featureDescriptor = commentingBuildFeature.get();
        final Map<String, String> parameters = featureDescriptor.getParameters();

        final String username = parameters.get("username");
        final String token = parameters.get("token");
        final String password = parameters.get("password");
        final String url = build.getVcsRootEntries().get(0).getProperties().get("url");
        final String[] urlParts = url.split("/");
        final String repoName = urlParts[urlParts.length - 2] + "/" + urlParts[urlParts.length - 1].replace(".git", "");

        try
        {
            final Integer pullId = Integer.parseInt(parameters.get("branch"));

            try
            {

                final GitHub github = new GitHubBuilder().withPassword(username, password).build();

                if (!github.isCredentialValid())
                {
                    throw new IllegalAccessException("Could not authenticate configured user against GitHub.");
                }

                final GHRepository repo = github.getRepository(repoName);
                final GHPullRequest request = repo.getPullRequest(pullId);


            }
            catch (Exception e)
            {
                build.getBuildLog()
                  .error("FAILURE", e.getMessage(), Date.from(Instant.now()), e.getLocalizedMessage(), String.valueOf(openGithubCommentingBlock.getFlowId()),
                    ImmutableList.of());
            }
        }
        catch (NumberFormatException nfe)
        {
            build.getBuildLog().message("Cannot upload comments to Github. Branch is not a number.", Status.ERROR, MessageAttrs.serverMessage());
        }

        build.getBuildLog()
          .closeBlock("Github PR Commenting - Dismissing existing review",
            getClass().getName() + "-Dismiss",
            Date.from(Instant.now()),
            String.valueOf(openGithubCommentingBlock.getFlowId()));
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

        final String appId = parameters.get("appId");
        final String privateKeyPath = parameters.get("privateKey");
        final String url = runningBuild.getVcsRootEntries().get(0).getProperties().get("url");
        final String[] urlParts = url.split("/");
        final String repoOwnerName = urlParts[urlParts.length - 2];
        final String repoName = urlParts[urlParts.length - 1].replace(".git", "");;
        final String repoTarget = urlParts[urlParts.length - 2] + "/" + urlParts[urlParts.length - 1].replace(".git", "");

        String appToken = "";

        try
        {
            final Key privateKey = RSA.getPrivateKeyFromString(privateKeyPath);
            final String jws = Jwts.builder().setIssuer(appId).setIssuedAt(Date.from(Instant.now())).setExpiration(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES))).signWith(privateKey).compact();

            final GitHub requestInstallationGithubInstance = new GitHubBuilder().withJwtToken(jws).build();
            final GHApp app = requestInstallationGithubInstance.getApp();
            final GHAppInstallation installation = app.getInstallationByRepository(repoOwnerName, repoName);
            final GHAppInstallationToken token = installation.createToken(ImmutableMap.of(
              "checks", GHPermissionType.WRITE
            )).create();
            appToken = token.getToken();
        }
        catch (Exception e)
        {
            runningBuild.getBuildLog()
              .error("FAILURE", e.getMessage(), Date.from(Instant.now()), e.getLocalizedMessage(), String.valueOf(openGithubCommentingBlock.getFlowId()),
                ImmutableList.of());
        }

        if (appToken != "")
        {

            try
            {
                final Integer pullId = Integer.parseInt(parameters.get("branch"));

                try
                {

                    final GitHub checksGithub = new GitHubBuilder().withAppInstallationToken(appToken).build();

                    if (!checksGithub.isCredentialValid())
                    {
                        throw new IllegalAccessException("Could not authenticate configured user against GitHub.");
                    }

                    final GHRepository repo = checksGithub.getRepository(repoTarget);
                    final GHPullRequest request = repo.getPullRequest(pullId);

                    final GHCheckRunBuilder builder = repo.createCheckRun("Inspections", request.getMergeCommitSha());

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

                    final List<SBuild> buildsWithInspections =
                      activeInfos
                        .stream()
                        .filter(inspectionInfo -> {
                            final List<String[]> inspections = inspectionInfo.getInspections();
                            return inspections.stream().anyMatch(data -> Integer.parseInt(data[6]) > 0);
                        })
                        .map(InspectionInfo::getBuild)
                        .collect(Collectors.toList());

                    final Collection<String> changedFiles = getChangesForBuild(runningBuild, activeBuilds);
                    final int[] statisticsTotal = getStatistics(activeInfos);
                    final int diffErrors = statisticsTotal[4] - statisticsTotal[5];

                    final String bodyBuilder = new Heading("Analysis Complete", 4) + "\n"
                                                 + new Heading("Statistics:", 5) + "\n"
                                                 + createStatisticContent(statisticsTotal).build() + "\n"
                                                 + new Heading("More Information:", 5) + "\n"
                                                 + new UnorderedList<>(
                      buildsWithInspections
                        .stream()
                        .map(
                          build -> new Link(build.getBuildTypeName(),
                            String.format(INSPECTION_URL_PATTERN, build.getBuildId(), build.getBuildTypeId()))
                        )
                        .collect(Collectors.toList())
                    );
                    final GHCheckRunBuilder.Output output = new GHCheckRunBuilder.Output("Inspection results", bodyBuilder);

                    changedFiles.stream().forEach(file -> {
                        final FileInspectionData fileInspectionData = new FileInspectionData(file, activeInfos, idNameMap.keySet(), request);
                        fileInspectionData.process(output);
                    });

                    builder.withStatus(GHCheckRun.Status.COMPLETED);
                    builder.withCompletedAt(Date.from(Instant.now()));
                    builder.withConclusion(diffErrors < 0 ? GHCheckRun.Conclusion.SUCCESS : (diffErrors == 0) ? GHCheckRun.Conclusion.NEUTRAL : GHCheckRun.Conclusion.ACTION_REQUIRED);

                    final GHCheckRun run = builder.create();
                    runningBuild.getBuildLog().message("Uploaded check run: " + run.getName(), Status.NORMAL, MessageAttrs.serverMessage());
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
        }


        runningBuild.getBuildLog().closeBlock("Github PR Commenting", getClass().getName(), Date.from(Instant.now()), String.valueOf(openGithubCommentingBlock.getFlowId()));
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
                       {
                           return two == null ? new int[6] : two;
                       }

                       if (two == null)
                       {
                           return one;
                       }

                       return new int[] {one[0] + two[0], one[1] + two[1], one[2] + two[2], one[3] + two[3], one[4] + two[4], one[5] + two[5]};
                   }
                 );
    }
}
