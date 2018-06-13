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

package jetbrains.sample.permissions;

import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adds to model all proviects available for the current user, lists of global and project permissions.
 * Works after user press "View My Permissions" button on user profile page
 */
public class ShowPermissionsController extends BaseController {
    private final WebControllerManager myWebManager;
    private final PluginDescriptor myPluginDescriptor;

    public ShowPermissionsController(final SBuildServer server,
                                     final WebControllerManager webManager,
                                     final PluginDescriptor pluginDescriptor) {
        super(server);
        myWebManager = webManager;
        myPluginDescriptor = pluginDescriptor;
    }

    public void register(){
      myWebManager.registerController("/viewPermissions.html", this);
    }

    @Override
    @Nullable
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        //List of all project permissions
        List<Permission> projectPermissionList = new ArrayList<Permission>();
        for (Permission p: Permission.values()) {
          if (p.isProjectAssociationSupported()) {
            projectPermissionList.add(p);
          }
        }

        //List of all permissions without project ones
        List<Permission> globalPermissionList = new ArrayList<Permission>(Arrays.asList(Permission.values()));
        globalPermissionList.removeAll(projectPermissionList);

        params.put("projects", myServer.getProjectManager().getProjects());
        params.put("projectPermissions", projectPermissionList);
        params.put("globalPermissions", globalPermissionList);
        params.put("userName", SessionUser.getUser(request).getDescriptiveName());

        return new ModelAndView(myPluginDescriptor.getPluginResourcesPath("viewPermissions.jsp"), params);
    }
}
