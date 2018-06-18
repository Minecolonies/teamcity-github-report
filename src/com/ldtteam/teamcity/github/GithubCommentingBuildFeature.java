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

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.SBuildServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GithubCommentingBuildFeature extends BuildFeature
{

    private final SBuildServer sBuildServer;

    public GithubCommentingBuildFeature(final SBuildServer sBuildServer)
    {
        this.sBuildServer = sBuildServer;
    }

    public void register()
    {
        System.out.println("Regitering github build feature bean.");
        this.sBuildServer.registerExtension(GithubCommentingBuildFeature.class, GithubCommentingBuildFeature.class.getName(), this);
    }

    /**
     * Type of the feature, must be unique among all of the features.
     *
     * @return type of the feature
     */
    @NotNull
    @Override
    public String getType()
    {
        return getClass().getName();
    }

    /**
     * User presentable name of the feature.
     *
     * @return name of the feature to show in the user interface.
     */
    @NotNull
    @Override
    public String getDisplayName()
    {
        return "Github PR Commenting";
    }

    /**
     * @return absolute path to a JSP file or controller for editing parameters, should not include
     * context path.
     */
    @Nullable
    @Override
    public String getEditParametersUrl()
    {
        return null;
    }

    /**
     * Returns true if single build type can contain more than one build feature of this type.
     *
     * @return see above
     */
    @Override
    public boolean isMultipleFeaturesPerBuildTypeAllowed()
    {
        return false;
    }

    /**
     * @return true if this build feature has some code which should be executed on the agent during the build.
     * If false is returned then parameters of this feature will not be passed to an agent.
     *
     * @since 2017.2
     */
    @Override
    public boolean isRequiresAgent()
    {
        return false;
    }
}