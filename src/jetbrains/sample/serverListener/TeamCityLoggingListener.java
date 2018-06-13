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

package jetbrains.sample.serverListener;

import java.text.DateFormat;
import java.util.*;
import jetbrains.buildServer.Build;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.db.SQLRunnerEx;
import jetbrains.buildServer.serverSide.impl.codeInspection.InspectionInfo;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.VcsModification;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Sample listener of server events
 */
public class TeamCityLoggingListener extends BuildServerAdapter {

  private final List<String> myLog = new ArrayList<String>();
  private final Map<String, List<String>> myConfigurationLog = new HashMap<String, List<String>>();
  private final SBuildServer myBuildServer;

  public TeamCityLoggingListener(SBuildServer sBuildServer) {

    myBuildServer = sBuildServer;
  }

  public void register() {
    myBuildServer.addListener(this);
  }

  @Override
  public void agentRegistered(@NotNull SBuildAgent sBuildAgent, long l) {
    addToLog("Agent " + sBuildAgent.getName() + " registered");
  }

  @Override
  public void agentUnregistered(@NotNull SBuildAgent sBuildAgent) {
    addToLog("Agent " + sBuildAgent.getName() + " unregistered");
  }

  @Override
  public void agentRemoved(@NotNull SBuildAgent sBuildAgent) {
    addToLog("Agent " + sBuildAgent.getName() + " removed");
  }

  @Override
  public void buildTypeAddedToQueue(@NotNull SQueuedBuild queuedBuild) {
    final SBuildType buildTypeDescriptor = queuedBuild.getBuildType();
    addToLog("Configuration  " + buildTypeDescriptor.getFullName() + " added to queue", buildTypeDescriptor.getBuildTypeId());
  }

  @Override
  public void buildRemovedFromQueue(@NotNull SQueuedBuild queuedBuild, User user, String comment) {
    SBuildType buildType = queuedBuild.getBuildType();
    addToLog("Configuration  " + buildType.getFullName() + " removed from queue", buildType.getBuildTypeId());
  }

  @Override
  public void buildQueueOrderChanged() {
    addToLog("Build configurations order changed");
  }

  @Override
  public void buildTypeRegistered(@NotNull SBuildType buildTypeDescriptor) {
    addToLog("Configuration  " + buildTypeDescriptor.getFullName() + " registered", buildTypeDescriptor.getBuildTypeId());
  }

  @Override
  public void buildTypeUnregistered(@NotNull SBuildType buildTypeDescriptor) {
    addToLog("Configuration  " + buildTypeDescriptor.getFullName() + " unregistered", buildTypeDescriptor.getBuildTypeId());
  }

  @Override
  public void buildTypeActiveStatusChanged(@NotNull SBuildType buildTypeDescriptor) {
    addToLog("Configuration  " + buildTypeDescriptor.getFullName() + (buildTypeDescriptor.isPaused() ? " paused" : " unpaused"),
             buildTypeDescriptor.getBuildTypeId());
  }

  @Override
  public void buildStarted(@NotNull SRunningBuild sRunningBuild) {
    addToLog("Build " + sRunningBuild.getFullName() + " started", sRunningBuild);
  }

  @Override
  public void changesLoaded(@NotNull SRunningBuild sRunningBuild) {
    addToLog("Changes loaded for build " + sRunningBuild.getFullName(), sRunningBuild);
  }

  @Override
  public void buildChangedStatus(@NotNull SRunningBuild sRunningBuild, Status status, Status status1) {
    addToLog("Build " + sRunningBuild.getFullName() + " changed status", sRunningBuild);
  }

  @Override
  public void buildFinished(@NotNull SRunningBuild sRunningBuild) {
    addToLog("Build " + sRunningBuild.getFullName() + " finished", sRunningBuild);
  }

  @Override
  public void beforeBuildFinish(@NotNull SRunningBuild sRunningBuild) {
    addToLog("Build " + sRunningBuild.getFullName() + " is going to finish", sRunningBuild);

    final InspectionInfo info = getInspectionInfo(sRunningBuild.getBuildId());
    final List<String[]> inspectionInformation = info.getInspections();

    addToLog(String.format("Build created: %d inspection entries.", inspectionInformation.size()), sRunningBuild);
    info.getInspections().stream().map(Arrays::toString).forEach(s -> addToLog(s, sRunningBuild));
  }

    @Nullable
    private InspectionInfo getInspectionInfo(long buildId) {
        SBuild build = myBuildServer.findBuildInstanceById(buildId);
        if (build == null) {
            return null;
        } else {
            BuildTypeEx buildType = (BuildTypeEx)build.getBuildType();
            return buildType == null ? null : new InspectionInfo((SQLRunnerEx) myBuildServer.getSQLRunner(), build);
        }
    }

  @Override
  public void responsibleChanged(@NotNull SBuildType sBuildType,
                                 @NotNull ResponsibilityEntry responsibilityInfo,
                                 @NotNull ResponsibilityEntry responsibilityInfo1) {
    addToLog("Responsible changed for " + sBuildType.getFullName(), sBuildType.getBuildTypeId());
  }

  @Override
  public void entryDeleted(@NotNull SFinishedBuild sFinishedBuild) {
    addToLog("Build " + sFinishedBuild.getFullName() + " deleted", sFinishedBuild);
  }

  @Override
  public void projectCreated(@NotNull String s, SUser user) {
    addToLog("Project " + s + " created");
  }

  @Override
  public void projectRemoved(@NotNull final SProject project) {
    addToLog("Project " + project.getName() + " removed");
  }

  @Override
  public void buildInterrupted(@NotNull SRunningBuild sRunningBuild) {
    addToLog("Build " + sRunningBuild.getFullName() + " interrupted", sRunningBuild);
  }

  @Override
  public void changeAdded(@NotNull final VcsModification modification,
                          @NotNull final VcsRoot root,
                          @Nullable final Collection<SBuildType> buildTypes) {
    addToLog("Change added to " + root.getName());
  }

  @Override
  public void agentStatusChanged(@NotNull SBuildAgent sBuildAgent, boolean wasEnabled, final boolean wasAuthorized) {
    addToLog("Agent " + sBuildAgent.getName() + " changed status");
  }

  private void addToLog(String message) {
    synchronized (myLog) {
      addToLog(myLog, message);
    }
  }

  private void addToLog(String message, Build build) {
    addToLog(message, build.getBuildTypeId());
  }

  private void addToLog(String message, String buildConfigId) {
    synchronized (myConfigurationLog) {
      if (!myConfigurationLog.containsKey(buildConfigId)) {
        myConfigurationLog.put(buildConfigId, new ArrayList<String>());
      }
      addToLog(myConfigurationLog.get(buildConfigId), message);
    }
    addToLog(message);
  }

  private void addToLog(List<String> log, String message) {
    while (log.size() > 9) {
      log.remove(log.size() - 1);
    }
    log.add(0, DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()) + ": " + message);
  }

  public List<String> getMessages() {
    synchronized (myLog) {
      return new ArrayList<String>(myLog);
    }
  }

  public boolean hasLogFor(SBuildType buildType) {
    synchronized (myConfigurationLog) {
      return myConfigurationLog.get(buildType.getBuildTypeId()) != null;
    }
  }

  public List<String> getLogFor(SBuildType buildType) {
    synchronized (myConfigurationLog) {
      return new ArrayList<String>(myConfigurationLog.get(buildType.getBuildTypeId()));
    }
  }
}
