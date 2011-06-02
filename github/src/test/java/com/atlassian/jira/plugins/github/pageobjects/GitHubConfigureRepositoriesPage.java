package com.atlassian.jira.plugins.github.pageobjects;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 * TODO: Document this class / interface here
 */
public class GitHubConfigureRepositoriesPage implements Page
{
    @ElementBy(id="gh_submit")
    PageElement addRepositoryButton;

    public String getUrl()
    {
        return "/secure/admin/GitHubConfigureRepositories!default.jspa";
    }

    @WaitUntil
    private void waitUntilReady()
    {
        Poller.waitUntilTrue(addRepositoryButton.timed().isPresent());
    }

    
}
