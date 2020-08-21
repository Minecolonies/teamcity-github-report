package com.ldtteam.teamcity.github.commenting;

import com.google.common.collect.ImmutableMap;
import com.ldtteam.teamcity.github.utils.RSA;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GithubCommentingBuildFeature extends BuildFeature
{

    private final SBuildServer sBuildServer;
    private final PluginDescriptor descriptor;

    public GithubCommentingBuildFeature(final SBuildServer sBuildServer, final PluginDescriptor descriptor)
    {
        this.sBuildServer = sBuildServer;
        this.descriptor = descriptor;
    }

    public void register()
    {
        System.out.println("Registering github build feature bean.");
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
        return descriptor.getPluginResourcesPath("editGithubPRCommentingFeatureView.jsp");
    }

    @NotNull
    @Override
    public String describeParameters(@NotNull final Map<String, String> params)
    {
        if (!params.containsKey("username"))
            return "Username: NOT SET!";

        return "User: " + params.get("username");
    }

    @Nullable
    @Override
    public PropertiesProcessor getParametersProcessor()
    {
        return properties -> {
            final List<InvalidProperty> errors = new ArrayList<>();
            if (true)
                return errors;

            if (!properties.containsKey("privateKey"))
                errors.add(new InvalidProperty("privateKey", "Private Key is not specified."));

            if (!properties.containsKey("appId"))
                errors.add(new InvalidProperty("appId", "Github App Id is not specified."));

            if (!properties.containsKey("branch"))
                errors.add(new InvalidProperty("branch", "Branch is missing or not specified."));

            if (!errors.isEmpty())
                return errors;

            try
            {
                RSA.getPrivateKeyFromString(properties.get("privateKey"));
            }
            catch (Exception e)
            {
                errors.add(new InvalidProperty("privateKey", "Failed to read the private key: " + e.getMessage()));
            }

            return errors;
        };
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultParameters()
    {
        return ImmutableMap.of("appId", "00000", "privateKey", "/secrets/teamcity-github-report.pem","branch", "0");
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
