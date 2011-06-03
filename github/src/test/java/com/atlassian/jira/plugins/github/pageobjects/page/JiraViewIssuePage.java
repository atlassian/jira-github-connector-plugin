package com.atlassian.jira.plugins.github.pageobjects.page;

import com.atlassian.jira.plugins.github.pageobjects.component.GitHubIssuePanel;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;

import javax.inject.Inject;

/**
 * Represents the JIRA view issue page
 */
public class JiraViewIssuePage implements Page
{
    @Inject
    PageBinder pageBinder;

    private final String issueKey;

    public JiraViewIssuePage(String issueKey)
    {
        this.issueKey = issueKey;
    }

    public String getUrl()
    {
        return "/browse/" + issueKey;
    }

    /**
     * Opens the github panel
     * @return GitHubIssuePanel
     */
    public GitHubIssuePanel openGitHubPanel()
    {
        return pageBinder.bind(GitHubIssuePanel.class).open();
    }
}
