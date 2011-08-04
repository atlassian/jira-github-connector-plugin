package it.com.atlassian.jira.plugins.github;

import com.atlassian.jira.plugins.github.pageobjects.page.GitHubConfigureRepositoriesPage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import org.junit.After;
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
        jira = TestedProductFactory.create(JiraTestedProduct.class);

        configureRepos = jira.gotoLoginPage().loginAsSysAdmin(GitHubConfigureRepositoriesPage.class);
    }

    @After
    public void logout()
    {
        jira.getTester().getDriver().manage().deleteAllCookies();
    }

}
