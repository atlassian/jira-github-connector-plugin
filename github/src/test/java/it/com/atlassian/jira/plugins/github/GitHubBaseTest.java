package it.com.atlassian.jira.plugins.github;

import com.atlassian.jira.plugins.github.pageobjects.page.GitHubConfigureRepositoriesPage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import org.junit.Before;

/**
 * Base class for GitHub integration tests. Initializes the JiraTestedProduct and logs admin in.
 */
public abstract class GitHubBaseTest
{
    protected JiraTestedProduct jira;
    protected GitHubConfigureRepositoriesPage configureRepos;

    @Before
    public void loginToJira()
    {
        System.setProperty("baseurl.jira", "http://localhost:2990/jira");
        System.setProperty("http.jira.port", "2990");
        System.setProperty("context.jira.path", "jira");

        jira = TestedProductFactory.create(JiraTestedProduct.class);

        if(jira.visit(HomePage.class).getHeader().isLoggedIn())
        {
            configureRepos = jira.visit(GitHubConfigureRepositoriesPage.class);
        }
        else
        {
            configureRepos = jira.gotoLoginPage().loginAsSysAdmin(GitHubConfigureRepositoriesPage.class);
        }
    }

    
}
