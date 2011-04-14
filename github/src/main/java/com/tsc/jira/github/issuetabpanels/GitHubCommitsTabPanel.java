package com.tsc.jira.github.issuetabpanels;

import com.atlassian.core.util.StringUtils;
import com.atlassian.core.util.collection.EasyList;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.opensymphony.user.User;
import com.tsc.jira.github.webwork.GitHubCommits;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GitHubCommitsTabPanel extends AbstractIssueTabPanel {

    final PluginSettingsFactory pluginSettingsFactory;

    public GitHubCommitsTabPanel(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void populateVelocityParams(Map params)
    {
        params.put("stringUtils", new StringUtils());
        params.put("github", this);
    }

    public List getActions(Issue issue, User user) {
        String projectKey = issue.getProjectObject().getKey();
        String issueId = (String)issue.getKey();

        GitHubCommits gitHubCommits = new GitHubCommits(pluginSettingsFactory);

        ArrayList<String> commitArray = new ArrayList<String>();

        String issueCommitActions = "No GitHub Commits Found";

        ArrayList<Object> githubActions = new ArrayList<Object>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueCommitArray" + issueId) != null){
            commitArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueCommitArray" + issueId);

            for (int i=0; i < commitArray.size(); i++){
                    System.out.println("Found commit id" + commitArray.get(i));
                    String commitDetails = gitHubCommits.getCommitDetails(commitArray.get(i));

                    issueCommitActions = this.formatCommitDetails(commitDetails);
                    GenericMessageAction action = new GenericMessageAction(issueCommitActions);
                    githubActions.add(action);

                    System.out.println("Commit Entry: " + "githubIssueCommitArray" + i );

            }



        }

        return EasyList.build(githubActions);

    }

    public boolean showPanel(Issue issue, User user) {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private String formatCommitDetails(String jsonDetails){
            try{
                JSONObject jsonCommits = new JSONObject(jsonDetails);
                JSONObject commit = jsonCommits.getJSONObject("commit");

                String message = commit.getString("message");
                String commit_id = commit.getString("id");

                String htmlAdded = "";

                if(commit.has("added")){
                    JSONArray arrayAdded = commit.getJSONArray("added");

                    htmlAdded = "<table><tr><th><font color='green'>Added</font></th></tr>";

                    for (int i=0; i < arrayAdded.length(); i++){
                          String addFilename = arrayAdded.getString(i);
                          htmlAdded += "<tr><td style='padding-left: 20px'>" + addFilename + "</td></tr>";
                    }

                    htmlAdded += "</table>";

                }

                String htmlRemoved = "";

                if(commit.has("removed")){
                    JSONArray arrayRemoved = commit.getJSONArray("removed");

                    htmlRemoved = "<table><tr><th><font color='red'>Removed</font></th></tr>";

                    for (int i=0; i < arrayRemoved.length(); i++){
                          String addFilename = arrayRemoved.getString(i);
                          htmlRemoved += "<tr><td style='padding-left: 20px'>" + addFilename + "</td></tr>";
                    }

                    htmlRemoved += "</table>";

                }

                String htmlModified = "";

                if(commit.has("modified")){
                    JSONArray arrayModified = commit.getJSONArray("modified");

                    htmlModified = "<table><tr><th colspan='2'><font color='blue'>Modified</font></th></tr>";

                    for (int i=0; i < arrayModified.length(); i++){
                          String modFilename = arrayModified.getJSONObject(i).getString("filename");
                          String modDiff = arrayModified.getJSONObject(i).getString("diff");
                          htmlModified += "<tr><td style='padding-left: 20px'>" + modFilename + " - " + modDiff + "</td></tr>";
                    }

                    htmlModified += "</table>";
                }

                JSONObject author = commit.getJSONObject("author");
                String authorName = author.getString("name");
                String login = author.getString("login");
                String commitURL = commit.getString("url");
                String committedDate = commit.getString("committed_date");

                String commitTree = commit.getString("tree");
                String commitMessage = commit.getString("message");
                JSONObject githubUser = new JSONObject(getUserDetails(login));
                JSONObject user = githubUser.getJSONObject("user");
                String gravatarHash = user.getString("gravatar_id");
                String gravatarUrl = "http://www.gravatar.com/avatar/" + gravatarHash + "?s=40";

                String htmlCommitTable = "<table><tr><th bgcolor=''><font color=''><strong>Commit:</strong></font></td><td bgcolor='' ><font color=''><a href='https://github.com" + commitURL + "'>" + commit_id + "</a></font></td></tr>";
                htmlCommitTable += "<tr><td><strong>Tree:</strong></td><td>" + commitTree + "</td></tr></table>";


                String htmlUserTable = "<table><tr><td><img src='" + gravatarUrl + "'></td>";
                htmlUserTable += "<td><strong><a href='https://github.com/"+ login + "'>" + login + "</a></strong><br/>" + committedDate + "</td></tr></table><hr size='5'>";

                return htmlCommitTable + commitMessage + htmlAdded + htmlRemoved + htmlModified + htmlUserTable;

            }catch (JSONException e){
                e.printStackTrace();
                return "exception";
            }

    }

    private String getUserDetails(String loginName){

        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL("http://github.com/api/v2/json/user/show/" + loginName);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        }catch (MalformedURLException e){
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;


    }


}
