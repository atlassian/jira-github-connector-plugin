package com.atlassian.jira.plugins.github.links;

import com.atlassian.jira.config.properties.PropertiesManager;
import com.atlassian.jira.plugin.projectoperation.AbstractPluggableProjectOperation;
import com.atlassian.jira.project.Project;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.opensymphony.user.User;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectSettings extends AbstractPluggableProjectOperation
{

    private final PluginSettingsFactory pluginSettingsFactory;
    private static final Pattern GITHUB_NAME_PATTERN = Pattern.compile(".*github.com/([^/]+/[^/]+)(/master)?");

    public ProjectSettings(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    public String getHtml(final Project project, final User user)
    {

        String baseURL = PropertiesManager.getInstance().getPropertySet().getString("jira.baseurl");

        List<String> repositories = getRepositories(project.getKey());
        StringBuilder result = new StringBuilder();
        result.append("<span class=\"project-config-list-label\">");
        if (repositories.size() > 1)
        {
            result.append("GitHub Repositories:");
        }
        else
        {
            result.append("GitHub Repository:");
        }
        result.append("</span>\n")
                .append("<span class=\"project-config-list-value\">");

        switch (repositories.size())
        {
            case 0:
                result.append("None");
                break;
            case 1:
                result.append(getRepositoryName(repositories.get(0)));
                break;
            default:
                result.append(repositories.size()).append(" repositories");
        }
        result.append(" (<a href='")
                .append(baseURL)
                .append("/secure/admin/GitHubConfigureRepositories!default.jspa?projectKey=")
                .append(project.getKey())
                .append("&mode=single'>")
                .append("Configure</a>)");
        return result.toString();

    }

    /**
     * Tries to extract repository name from URL
     * @param repoUrl The repo url, shouldn't be null, but could be
     * @return The text to tell the user about this repo
     */
    String getRepositoryName(String repoUrl)
    {
        String result = "One repository";
        if (repoUrl != null)
        {
            Matcher matcher = GITHUB_NAME_PATTERN.matcher(repoUrl);
            if (matcher.matches())
            {
                result = matcher.group(1);
            }
        }
        return result;
    }

    private List<String> getRepositories(String projectKey)
    {
        List<String> repoUrls = (List<String>) pluginSettingsFactory.createSettingsForKey(projectKey).get("githubRepositoryURLArray");
        return repoUrls != null ? repoUrls : Collections.<String>emptyList();
    }

    public boolean showOperation(final Project project, final User user)
    {
        return true;
    }
}
