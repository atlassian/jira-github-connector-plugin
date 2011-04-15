package com.tsc.jira.github.issuetabpanels;

import com.atlassian.core.util.StringUtils;
import com.atlassian.core.util.collection.EasyList;
import com.atlassian.gadgets.view.View;
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

import com.tsc.jira.github.webwork.ViewProjectRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.text.ParseException;



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
        gitHubCommits.projectKey = projectKey;

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

    private Date parseISO8601(String input) throws ParseException{
        //NOTE: SimpleDateFormat uses GMT[-+]hh:mm for the TZ which breaks
        //things a bit.  Before we go on we have to repair this.
        SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz" );

        //this is zero time so we need to add that TZ indicator for
        if ( input.endsWith( "Z" ) ) {
            input = input.substring( 0, input.length() - 1) + "GMT-00:00";
        } else {
            int inset = 6;

            String s0 = input.substring( 0, input.length() - inset );
            String s1 = input.substring( input.length() - inset, input.length() );

            input = s0 + "GMT" + s1;
        }

        return df.parse(input);

    }

    private String formatCommitDate(Date commitDate) throws ParseException{
        SimpleDateFormat sdfGithub = new SimpleDateFormat("MMM d, yyyy k:ma");
        return sdfGithub.format(commitDate);
    }

    public String extractDiffInformation(String diff){

        // the +3 and -1 remove the leading and trailing spaces
        Integer first = diff.indexOf("@@") + 3;
        Integer second = diff.indexOf("@@", first) -1;

        String[] modLine = diff.substring(first,second).replace("+","").replace("-","").split(" ");

        String[] removedEntryArray = modLine[0].split(",");
        String[] addedEntryArray = modLine[1].split(",");

        String removedEntry = "";
        String addedEntry = "";

        if (removedEntryArray.length == 1){
            removedEntry = removedEntryArray[0];
        }else{
            removedEntry = removedEntryArray[1];
        }

        if (addedEntryArray.length == 1){
            addedEntry = addedEntryArray[0];
        }else{
            addedEntry = addedEntryArray[1];
        }

        if (addedEntry == "0"){
            addedEntry = "<span style='color: gray'>+" + addedEntry + "</span>";
        }else{
            addedEntry = "<span style='color: green'>+" + addedEntry + "</span>";
        }

        if (removedEntry == "0"){
            removedEntry = "<span style='color: gray'>-" + removedEntry + "</span>";
        }else{
            removedEntry = "<span style='color: red'>-" + removedEntry + "</span>";
        }


        return addedEntry + " " + removedEntry;

    }

    private String formatCommitDetails(String jsonDetails){
            try{
                JSONObject jsonCommits = new JSONObject(jsonDetails);
                JSONObject commit = jsonCommits.getJSONObject("commit");

                String message = commit.getString("message");
                String commit_hash = commit.getString("id");

                JSONObject author = commit.getJSONObject("author");
                String authorName = author.getString("name");
                String login = author.getString("login");
                String commitURL = commit.getString("url");

                String[] commitURLArray = commitURL.split("/");

                String projectName = commitURLArray[2];

                String committedDateString = commit.getString("committed_date");

                String formattedCommitDate = "";

                try{
                    Date committedDate = parseISO8601(committedDateString);
                    formattedCommitDate = formatCommitDate(committedDate);
                }catch (ParseException pe){

                }

                String commitTree = commit.getString("tree");
                String commitMessage = commit.getString("message");
                JSONObject githubUser = new JSONObject(getUserDetails(login));
                JSONObject user = githubUser.getJSONObject("user");
                String userName = user.getString("name");
                String gravatarHash = user.getString("gravatar_id");
                String gravatarUrl = "http://www.gravatar.com/avatar/" + gravatarHash + "?s=40";

                String htmlParentHashes = "";

                if(commit.has("parents")){
                    JSONArray arrayParents = commit.getJSONArray("parents");

                    for (int i=0; i < arrayParents.length(); i++){
                        String parentHashID = arrayParents.getJSONObject(i).getString("id");
                        htmlParentHashes = "<tr><td>Parent:</td><td><a href='" + "https://github.com/" + login + "/" + projectName + "/commit/" + parentHashID +"' target='_new'>" + parentHashID + "</a></td></tr>";
                    }

                }

                String htmlAdded = "";

                if(commit.has("added")){
                    JSONArray arrayAdded = commit.getJSONArray("added");

                    htmlAdded = "<div style='color: green;'>Added</div><ul>";

                    for (int i=0; i < arrayAdded.length(); i++){
                          String addFilename = arrayAdded.getString(i);
                          htmlAdded += "<li>" + addFilename + "</li>";
                    }

                    htmlAdded += "</ul>";

                }

                String htmlRemoved = "";

                if(commit.has("removed")){
                    JSONArray arrayRemoved = commit.getJSONArray("removed");

                    htmlRemoved = "<div style='color: red;'>Removed</div><ul>";

                    for (int i=0; i < arrayRemoved.length(); i++){
                          String removeFilename = arrayRemoved.getString(i);
                          htmlRemoved += "<li>" + removeFilename + "</li>";
                    }

                    htmlRemoved += "</ul>";

                }

                String htmlModified = "";

                if(commit.has("modified")){
                    JSONArray arrayModified = commit.getJSONArray("modified");

                    htmlModified = "<div style='color: blue;'>Modified</div><ul>";

                    for (int i=0; i < arrayModified.length(); i++){
                          String modFilename = arrayModified.getJSONObject(i).getString("filename");
                          String modDiff = arrayModified.getJSONObject(i).getString("diff");
                          htmlModified += "<li>" + extractDiffInformation(modDiff) + " " + modFilename + "</li>";
                    }

                    htmlModified += "</ul>";
                }




String htmlCommitEntry = "" +
    "<table>" +
        "<tr>" +
            "<td valign='top'><a href='#user_url' target='_new'><img src='#gravatar_url' border='0'></a></td>" +
            "<td valign='top'>" +
                "<div><a href='#user_url' target='_new'>#user_name - #login</a></div>" +
                "<table>" +
                    "<tr>" +
                        "<td>" +
                            "<div style='border-left: 4px solid #C9D9EF; background-color: #EAF3FF; padding: 5px; margin-bottom: 10px;'>#commit_message</div>" +

                                htmlAdded +
                                htmlRemoved +
                                htmlModified +

                            "<div>" +
                                "<img src='#resource_page_image' align='center'> <span style='color: #cccccc'>#formatted_commit_date</span>" +
                            "</div>" +

                        "</td>" +

                        "<td>" +
                            "<div style='border-left: 2px solid #cccccc; margin-left: 10px; margin-top: 0px; padding-top: 0px;'>" +
                                "<table style='margin-top: -20px; padding-top: 0px;'>" +
                                    "<tr><td>Commit:</td><td><a href='#commit_url' target='_new'>#commit_hash</a></td></tr>" +
                                     "<tr><td>Tree:</td><td><a href='#tree_url' target='_new'>#tree_hash</a></td></tr>" +
                                     htmlParentHashes +
                                "</table>" +
                            "</div>" +
                        "</td>" +

                    "</tr>" +
                "</table>" +
        "</td>" +
    "</tr>" +
"</table>";


                htmlCommitEntry = htmlCommitEntry.replace("#gravatar_url", gravatarUrl);
                htmlCommitEntry = htmlCommitEntry.replace("#user_url", "https:github.com/" + login);
                htmlCommitEntry = htmlCommitEntry.replace("#login", login);

                htmlCommitEntry = htmlCommitEntry.replace("#user_name", userName);

                htmlCommitEntry = htmlCommitEntry.replace("#commit_message", commitMessage);

                htmlCommitEntry = htmlCommitEntry.replace("#formatted_commit_time", committedDateString);
                htmlCommitEntry = htmlCommitEntry.replace("#formatted_commit_date", formattedCommitDate);

                htmlCommitEntry = htmlCommitEntry.replace("#commit_url", "https://github.com" + commitURL);
                htmlCommitEntry = htmlCommitEntry.replace("#commit_hash", commit_hash);

                htmlCommitEntry = htmlCommitEntry.replace("#tree_url", "https://github.com/" + login + "/" + projectName + "/tree/" + commit_hash);

                htmlCommitEntry = htmlCommitEntry.replace("#tree_hash", commitTree);
                return htmlCommitEntry;

            }catch (JSONException e){
                e.printStackTrace();
                return "Invalid or removed GitHub Commit ID found";
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
