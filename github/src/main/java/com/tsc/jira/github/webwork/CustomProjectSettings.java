package com.tsc.jira.github.webwork;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class CustomProjectSettings{
    final PluginSettingsFactory pluginSettingsFactory;

    public CustomProjectSettings(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    public void storeSomeInfo(String key, String value) {
        // createGlobalSettings is nice and fast, so there's no need to cache it (it's memoised when necessary).
        pluginSettingsFactory.createGlobalSettings().put("github" + key, value);
    }

    public Object getSomeInfo(String key) {
        return pluginSettingsFactory.createGlobalSettings().get("github" + key);
    }

    public void storeSomeInfo(String projectKey, String key, String value) {
        // createSettingsForKey is nice and fast, so there's no need to cache it (it's memoised when necessary).
        pluginSettingsFactory.createSettingsForKey(projectKey).put("github" + key, value);
    }

    public Object getSomeInfo(String projectKey, String key) {
        return pluginSettingsFactory.createSettingsForKey(projectKey).get("github" + key);
    }

}
