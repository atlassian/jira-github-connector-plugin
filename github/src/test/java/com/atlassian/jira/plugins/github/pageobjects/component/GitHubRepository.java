package com.atlassian.jira.plugins.github.pageobjects.component;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.openqa.selenium.By;

import javax.inject.Inject;

/**
 * Represents a repository that is linked to a project (a table row of <tt>GitHubConfigureRepositoriesPage</tt>)
 */
public class GitHubRepository
{
    private final PageElement row;

    @Inject
    AtlassianWebDriver driver;

    @Inject
    PageElementFinder elementFinder;

    public GitHubRepository(PageElement row)
    {
        this.row = row;
    }

    /**
     * The url of this repo
     * @return Url
     */
    public String getUrl()
    {
        return row.find(By.tagName("a")).getText();
    }

    /**
     * The projec key
     * @return Key
     */
    public String getProjectKey()
    {
        return row.findAll(By.tagName("td")).get(1).getText();
    }

    /**
     * Deletes this repository from the list
     */
    public void delete()
    {
        // disable confirm popup
        driver.executeScript("window.confirm = function(){ return true; }");

        // add marker to wait for post complete
        // TODO: it is possible that we attempt to execute this script before AJS is loaded on the page.
        // TODO: Need to add check for AJS on waituntil of host page
        driver.executeScript("AJS.$('#gh_submit').addClass('_posting')");
        row.find(By.linkText("Delete")).click();

        //wait until marker is gone.
        Poller.waitUntilFalse(elementFinder.find(By.id("gh_submit")).timed().hasClass("_posting"));
    }
}
