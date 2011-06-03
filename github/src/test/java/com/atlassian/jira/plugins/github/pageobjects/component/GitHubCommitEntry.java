package com.atlassian.jira.plugins.github.pageobjects.component;

import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

/**
 * Represents a commit entry that is displayed in the <tt>GitHubIssuePanel</tt>
 */
public class GitHubCommitEntry
{
    private final PageElement div;

    public GitHubCommitEntry(PageElement div)
    {
        this.div = div;
    }

    /**
     * The message associated with this commit
     * @return Message
     */
    public String getCommitMessage()
    {
        return div.findAll(By.tagName("div")).get(1).getText();
    }
}
