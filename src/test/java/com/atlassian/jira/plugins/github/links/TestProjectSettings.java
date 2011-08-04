package com.atlassian.jira.plugins.github.links;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.struts.action.PlugIn;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class TestProjectSettings
{
    private ProjectSettings projectSettings;
    PluginSettingsFactory pluginSettingsFactory;

    @Before
    public void setUp()
    {
        pluginSettingsFactory = mock(PluginSettingsFactory.class);
        projectSettings = new ProjectSettings(pluginSettingsFactory);
    }

    @Test
    public void testGithubNameParsing()
    {
        assertEquals("mrdon/speakeasy-plugin", projectSettings.getRepositoryName("https://github.com/mrdon/speakeasy-plugin/master"));
        assertEquals("mrdon/speakeasy-plugin", projectSettings.getRepositoryName("https://github.com/mrdon/speakeasy-plugin"));
        assertEquals("One repository", projectSettings.getRepositoryName("https://foo/bar/baz"));
        assertEquals("One repository", projectSettings.getRepositoryName("foo/bar"));
        assertEquals("One repository", projectSettings.getRepositoryName("foo/"));
        assertEquals("One repository", projectSettings.getRepositoryName(null));
        assertEquals("One repository", projectSettings.getRepositoryName("foo"));

    }
}
