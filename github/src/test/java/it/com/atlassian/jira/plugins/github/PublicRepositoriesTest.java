package it.com.atlassian.jira.plugins.github;

import com.atlassian.jira.plugins.github.pageobjects.GitHubConfigureRepositoriesPage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.pageobjects.page.WebSudoPage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import org.junit.Test;

/**
 * TODO: Document this class / interface here
 */
public class PublicRepositoriesTest
{
    @Test
    public void addPublicRepo()
    {
        JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);

        GitHubConfigureRepositoriesPage configureRepos = jira.visit(LoginPage.class)
                                                             .loginAsSysAdmin(WebSudoPage.class)
                                                             .confirm(GitHubConfigureRepositoriesPage.class);

        
    }
}
