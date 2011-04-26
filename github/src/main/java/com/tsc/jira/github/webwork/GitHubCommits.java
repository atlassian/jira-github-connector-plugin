package com.tsc.jira.github.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.MutableIssue;

public class GitHubCommits {

    public String repositoryURL;
    public String projectKey;

    final PluginSettingsFactory pluginSettingsFactory;

    public GitHubCommits(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    // Generates a URL for pulling commit messages based upon the base Repository URL
    private String inferCommitsURL(){
        String[] path = repositoryURL.split("/");
        return "https://github.com/api/v2/json/commits/list/" + path[3] + "/" + path[4] + "/" + path[5];
    }

    // Generate a URL for pulling a single commits details (diff and author)
    private String inferCommitDetailsURL(){
        String[] path = repositoryURL.split("/");
        return "https://github.com/api/v2/json/commits/show/" + path[3] + "/" + path[4] +"/";
    }

    private String getBranchFromURL(){
        String[] path = repositoryURL.split("/");
        return path[5];
    }

    // Only used for Private Github Repositories
    private String getAccessTokenParameter(){

        String accessToken = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubRepositoryAccessToken" + repositoryURL);

        if (accessToken == null){
            return "";
        }else{
            return "&access_token=" + accessToken;
        }

    }

    private String getCommitsList(Integer pageNumber){
        System.out.println("getCommitsList()");
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try {

            System.out.println("Commits URL - " + this.inferCommitsURL() + "?page=" + Integer.toString(pageNumber) + this.getAccessTokenParameter() );
            url = new URL(this.inferCommitsURL() + "?page=" + Integer.toString(pageNumber) + this.getAccessTokenParameter());
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
            System.out.println("CommitList Exception");
            //e.printStackTrace();
        }

        return result;
    }

    // Commit list returns id (hashed) and Message
    // you have to call each individual commit to get diff details
    public String getCommitDetails(String commit_id_url){
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(commit_id_url + this.getAccessTokenParameter());
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

    private String extractProjectKey(String message){
        Pattern projectKeyPattern = Pattern.compile("(" + this.projectKey + "-\\d*)");
        Matcher match = projectKeyPattern.matcher(message);
        Boolean boolFound = match.find();

        if(boolFound){
            return match.group(0);
        }else{
            return "";
        }
    }

    private Integer incrementCommitCount(String commitType){

        int commitCount;

        if (pluginSettingsFactory.createSettingsForKey(projectKey).get(commitType + repositoryURL) == null){
            commitCount = 0;
        }else{
            String stringCount = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get(commitType + repositoryURL);
            commitCount = Integer.parseInt(stringCount) + 1;
        }

        commitCount = commitCount + 1;

        pluginSettingsFactory.createSettingsForKey(projectKey).put(commitType + repositoryURL, Integer.toString(commitCount));

        return commitCount;

    }

    public String syncCommits(Integer pageNumber){

        Date date = new Date();
        pluginSettingsFactory.createSettingsForKey(projectKey).put("githubLastSyncTime" + repositoryURL, date.toString());

        System.out.println("searchCommits()");
        String commitsAsJSON = getCommitsList(pageNumber);

        String messages = "";

        Integer nonJIRACommits = 0;
        Integer JIRACommits = 0;

        if (commitsAsJSON != ""){

            try{
                JSONObject jsonCommits = new JSONObject(commitsAsJSON);
                JSONArray commits = jsonCommits.getJSONArray("commits");

                for (int i = 0; i < commits.length(); ++i) {
                    String message = commits.getJSONObject(i).getString("message");
                    String commit_id = commits.getJSONObject(i).getString("id");

                    // Detect presence of JIRA Issue Key
                    if (message.indexOf(this.projectKey) > -1){
                        if (!extractProjectKey(message).equals("")){

                            String issueId = extractProjectKey(message);
                            addCommitID(issueId, commit_id, getBranchFromURL());
                            incrementCommitCount("JIRACommitTotal");

                            JIRACommits++;

                            messages += "<div class='jira_issue'>" + issueId + " " + commit_id + "</div>";
                            //String commitDetailsJSON = getCommitDetails(commit_id);

                            //deleteCommitId(issueId);
                        }

                    }else{
                        incrementCommitCount("NonJIRACommitTotal");
                        nonJIRACommits++;
                        messages += "<div class='no_issue'>No Issue: " + commit_id + "</div>" ;
                    }
                }



                messages += this.syncCommits(pageNumber + 1);

            }catch (JSONException e){
                e.printStackTrace();
                return "exception";
            }

            String messageHeader = "<h2>Sync Summary</h2>";
            messageHeader += "<strong>Non JIRA Commits Found: </strong>" + nonJIRACommits.toString() + "<br/>";
            messageHeader += "<strong>JIRA Commits Found: </strong>" + JIRACommits.toString() + "<br/><p/>";

            return messageHeader + " " + messages;

        }

        return "";

    }



    public String postReceiveHook(String payload){

        Date date = new Date();
        pluginSettingsFactory.createSettingsForKey(projectKey).put("githubLastSyncTime" + repositoryURL, date.toString());

        System.out.println("postBack()");
        String messages = "";

        try{
            JSONObject jsonCommits = new JSONObject(payload);
            JSONArray commits = jsonCommits.getJSONArray("commits");

            for (int i = 0; i < commits.length(); ++i) {
                String message = commits.getJSONObject(i).getString("message");
                String commit_id = commits.getJSONObject(i).getString("id");

                // Detect presence of JIRA Issue Key
                if (message.indexOf(this.projectKey) > -1){
                    if (!extractProjectKey(message).equals("")){

                        String issueId = extractProjectKey(message);
                        addCommitID(issueId, commit_id, getBranchFromURL());
                        incrementCommitCount("JIRACommitTotal");

                        messages += "<div class='jira_issue'>" + issueId + " " + commit_id + "</div>";
                        //String commitDetailsJSON = getCommitDetails(commit_id);

                        //deleteCommitId(issueId);
                    }

                }else{
                    incrementCommitCount("NonJIRACommitTotal");
                    messages += "<div class='no_issue'>No Issue: " + commit_id + "</div>" ;
                }
            }


        }catch (JSONException e){
            e.printStackTrace();
            return "exception";
        }

        return messages;


    }

    // Manages the entry of multiple Github commit id hash ids associated with an issue
    // urls look like - https://github.com/api/v2/json/commits/show/mojombo/grit/5071bf9fbfb81778c456d62e111440fdc776f76c?branch=master
    private void addCommitID(String issueId, String commitId, String branch){
        ArrayList<String> commitArray = new ArrayList<String>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueCommitArray" + issueId) != null){
            commitArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueCommitArray" + issueId);
        }

        Boolean boolExists = false;

        for (int i=0; i < commitArray.size(); i++){
            if ((inferCommitDetailsURL() + commitId + "?branch=" + branch).equals(commitArray.get(i))){
                System.out.println("Found commit id" + commitArray.get(i));
                boolExists = true;
            }
        }

        if (!boolExists){
            System.out.println("addCommitID: Adding CommitID " + inferCommitDetailsURL() + commitId );
            commitArray.add(inferCommitDetailsURL() + commitId + "?branch=" + branch);
            pluginSettingsFactory.createSettingsForKey(projectKey).put("githubIssueCommitArray" + issueId, commitArray);
        }else{
            System.out.println("addCommitID: commit id already present");
        }

        System.out.println("arrayKey: " + "githubIssueCommitArray" + issueId);
        //System.out.println("addCommitID: " + issueId + " - " + commitId);

    }

    // Removes all of the associated commits from an issue
    private void deleteCommitId(String issueId){
        pluginSettingsFactory.createSettingsForKey(projectKey).put("githubIssueCommitArray" + issueId, null);
    }

}
