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

package jetbrains.sample.extension;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab;
import jetbrains.sample.serverListener.TeamCityLoggingListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigurationTabExtension extends BuildTypeTab {
  private final TeamCityLoggingListener myListener;

  public ConfigurationTabExtension(@NotNull WebControllerManager manager,
                                   @NotNull TeamCityLoggingListener listener,
                                   @NotNull ProjectManager projectManager) {
    super("samplePlugin", "Sample Extension", manager, projectManager);
    myListener = listener;
  }

  @Override
  public boolean isAvailable(@NotNull HttpServletRequest request) {
    SBuildType buildType = getBuildType(request);
    return buildType != null && super.isAvailable(request) && myListener.hasLogFor(buildType);
  }

  @Override
  protected void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request,
                           @NotNull SBuildType buildType, @Nullable SUser user) {
    model.put("messages", myListener.getLogFor(buildType));
  }
}
