package it.com.atlassian.jira.plugins.github;

import com.atlassian.jira.plugins.github.pageobjects.component.GitHubCommitEntry;
import com.atlassian.jira.plugins.github.pageobjects.page.JiraViewIssuePage;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test to verify behaviour when syncing public github repos.
 */
public class PublicRepositoriesTest extends GitHubBaseTest
{
    @Test
    public void addPublicRepo_VerifyAppearsInList()
    {
        configureRepos.deleteAllRepositories();
        
        configureRepos.addPublicRepoToProject("QA", "https://github.com/farmas/TestRepo-QA");

        assertEquals(1, configureRepos.getRepositories().size());
    }

    @Test
    public void addPublicRepo_VerifyCommitsOnIssues()
    {
        if(configureRepos.isRepositoryPresent("QA", "https://github.com/farmas/TestRepo-QA/master") == false)
        {
            configureRepos.addPublicRepoToProject("QA", "https://github.com/farmas/TestRepo-QA");
        }

        List<GitHubCommitEntry> commitList = jira.visit(JiraViewIssuePage.class, "QA-1")
                                                          .openGitHubPanel()
                                                          .waitForMessages();

        assertTrue("Expected at least 1 commit entry", commitList.size() > 1);

        //verify message of first commit
        assertEquals("GH add 1 file to QA-1", commitList.get(commitList.size()-1).getCommitMessage());

    }
}
