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

package jetbrains.sample.controller;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.sample.serverListener.TeamCityLoggingListener;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

/**
 * Our sample controller
 */
public class HelloUserController extends BaseController {
    private final WebControllerManager myManager;
    private final TeamCityLoggingListener myLoggingListener;
    private final PluginDescriptor myPluginDescriptor;


    public HelloUserController(final SBuildServer sBuildServer,
                               final WebControllerManager manager,
                               final TeamCityLoggingListener listener,
                               final PluginDescriptor pluginDescriptor) {
        super(sBuildServer);
        myManager = manager;
        myLoggingListener = listener;
        myPluginDescriptor = pluginDescriptor;
    }

    /**
     * Main method which works after user presses 'Hello' button.
     *
     * @param httpServletRequest  http request
     * @param httpServletResponse http response
     * @return object containing model object and view (page address)
     * @throws Exception
     */
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest httpServletRequest, @NotNull HttpServletResponse httpServletResponse) throws Exception {
        SUser user = SessionUser.getUser(httpServletRequest);
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("userName", user.getDescriptiveName());
        params.put("messages", myLoggingListener.getMessages());
        return new ModelAndView(myPluginDescriptor.getPluginResourcesPath("hello.jsp"), params);
    }

    public void register() {
        myManager.registerController("/helloUser.html", this);
    }

}
