package com.atlassian.jira.plugins.github.pageobjects.component;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.components.ActivatedComponent;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the GitHub panel in the view issue page
 */
public class GitHubIssuePanel implements ActivatedComponent<GitHubIssuePanel>
{
    @Inject
    PageBinder pageBinder;

    @ElementBy(id="github-commits-tabpanel")
    PageElement trigger;

    @ElementBy(id="issue_actions_container")
    PageElement view;

    public PageElement getTrigger()
    {
        return trigger;
    }

    public PageElement getView()
    {
        return view;
    }

    public GitHubIssuePanel open()
    {
        if(!isOpen())
        {
            trigger.click();
            Poller.waitUntilTrue(trigger.timed().hasClass("active"));
        }
        return this;
    }

    public boolean isOpen()
    {
        return trigger.hasClass("active");
    }

    /**
     * Waits for commits to be retrieved from GitHub
     * @return List of <tt>GitHubCommitEntry</tt>
     */
    public List<GitHubCommitEntry> waitForMessages()
    {
        // wait for one message to be present (setting timeout type to longest value)
        Poller.waitUntilTrue(view.find(By.className("message-container"), TimeoutType.PAGE_LOAD).timed().isPresent());

        //get all the messages
        List<GitHubCommitEntry> commitMessageList = new ArrayList<GitHubCommitEntry>();
        for(PageElement div : view.findAll(By.className("message-container")))
        {
            commitMessageList.add(pageBinder.bind(GitHubCommitEntry.class, div));
        }

        return commitMessageList;
    }
}
