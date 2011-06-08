package com.atlassian.jira.plugins.github.pageobjects.page;

import com.atlassian.jira.plugins.github.pageobjects.component.GitHubRepository;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.*;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the page to link repositories to projects
 */
public class GitHubConfigureRepositoriesPage implements Page
{
    @Inject
    PageBinder pageBinder;

    @ElementBy(id="gh_submit")
    PageElement addRepositoryButton;

    @ElementBy(name = "projectKey")
    SelectElement projectSelect;

    @ElementBy(id = "url")
    PageElement urlTextbox;

    @ElementBy(id = "repoVisibility")
    SelectElement visibilitySelect;

    @ElementBy(id = "connector_sync_status")
    PageElement syncStatusDiv;

    @ElementBy(className = "gh_table")
    PageElement projectsTable;

    public String getUrl()
    {
        return "/secure/admin/GitHubConfigureRepositories!default.jspa";
    }

    @WaitUntil
    private void waitUntilReady()
    {
        Poller.waitUntilTrue(addRepositoryButton.timed().isPresent());
    }

    /**
     * Links a public repository to the given JIRA project
     * @param projectKey The JIRA project key
     * @param url The url to the github public repo
     * @return GitHubConfigureRepositoriesPage
     */
    public GitHubConfigureRepositoriesPage addPublicRepoToProject(String projectKey, String url)
    {
         projectSelect.select(Options.value(projectKey));
        visibilitySelect.select(Options.value("public"));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();

        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());

        Poller.waitUntilTrue("Expected sync status message to be 'Sync Processing Complete'",
                syncStatusDiv.find(By.tagName("strong")).timed().hasText("Sync Processing Complete"));

        return this;
    }

    /**
     * Returns a list of <tt>GitHubRepository</tt> with the current list of repositories linked.
     * @return List of <tt>GitHubRepository</tt>
     */
    public List<GitHubRepository> getRepositories()
    {
        List<GitHubRepository> list = new ArrayList<GitHubRepository>();
        for(PageElement row : projectsTable.findAll(By.tagName("tr")))
        {
            if(row.getText().contains("Force Sync"))
            {
                list.add(pageBinder.bind(GitHubRepository.class, row));
            }
        }

        return list;
    }

    /**
     * Deletes all repositories
     * @return GitHubConfigureRepositoriesPage
     */
    public GitHubConfigureRepositoriesPage deleteAllRepositories()
    {
        for(GitHubRepository repo : getRepositories())
        {
            repo.delete();
        }
        return this;
    }

    /**
     * Whether a repository is currently linked to a given project
     * @param projectKey The JIRA project key
     * @param url The repository url
     * @return True if repository is linked, false otherwise
     */
    public boolean isRepositoryPresent(String projectKey, String url)
    {
        boolean commitFound = false;
        for(GitHubRepository repo: getRepositories())
        {
            if(repo.getProjectKey().equals(projectKey) && repo.getUrl().equals(url))
            {
                commitFound = true;
                break;
            }
        }

        return commitFound;
    }
}
