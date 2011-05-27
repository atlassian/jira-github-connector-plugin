package com.atlassian.jira.plugins.github.webwork;

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
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.MutableIssue;

public class GitHubCommits {

    public String repositoryURL;
    public String projectKey;

    final PluginSettingsFactory pluginSettingsFactory;
    final Logger logger = LoggerFactory.getLogger(GitHubCommits.class);

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
        logger.debug("getCommitsList()");
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try {

            logger.debug("Commits URL - " + this.inferCommitsURL() + "?page=" + Integer.toString(pageNumber) + this.getAccessTokenParameter() );
            url = new URL(this.inferCommitsURL() + "?page=" + Integer.toString(pageNumber) + this.getAccessTokenParameter());
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();

            // Sets current page status for UI feedback
            pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryURL + projectKey, pageNumber.toString());

        }catch (MalformedURLException e){
            e.printStackTrace();
            if(pageNumber.equals(1)){
                result = "GitHub Repository can't be found or incorrect credentials.";
            }

            pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryURL + projectKey, "complete");

        } catch (Exception e) {
            //logger.debug("CommitList Exception");
            if(pageNumber.equals(1)){
                result = "GitHub Repository can't be found or incorrect credentials.";
            }

            pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryURL + projectKey, "complete");

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

    private ArrayList extractProjectKey(String message){
        Pattern projectKeyPattern = Pattern.compile("(" + this.projectKey + "-\\d*)", Pattern.CASE_INSENSITIVE);
        Matcher match = projectKeyPattern.matcher(message);

        boolean matchFound = match.find();

        ArrayList<String> matches = new ArrayList<String>();

        if (matchFound) {
            // Get all groups for this match
            for (int i=0; i<=match.groupCount(); i++) {
                matches.add(match.group(i));
            }
        }

        return matches;
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

        logger.debug("searchCommits()");
        String commitsAsJSON = getCommitsList(pageNumber);

        String messages = "";

        Integer nonJIRACommits = 0;
        Integer JIRACommits = 0;

        if (commitsAsJSON != ""){

            try{
                JSONObject jsonCommits = new JSONObject(commitsAsJSON);
                JSONArray commits = jsonCommits.getJSONArray("commits");

                for (int i = 0; i < commits.length(); ++i) {
                    String message = commits.getJSONObject(i).getString("message").toLowerCase();
                    String commit_id = commits.getJSONObject(i).getString("id");

                    // Detect presence of JIRA Issue Key
                    if (message.indexOf(this.projectKey.toLowerCase()) > -1){

                        ArrayList extractedIssues = extractProjectKey(message);

                        // Remove duplicate IssueIDs
                        HashSet h = new HashSet(extractedIssues);
                        extractedIssues.clear();
                        extractedIssues.addAll(h);

                        for (int j=0; j < extractedIssues.size(); ++j){
                            String issueId = (String)extractedIssues.get(j).toString().toUpperCase();
                            addCommitID(issueId, commit_id, getBranchFromURL());
                            incrementCommitCount("JIRACommitTotal");

                            JIRACommits++;

                            messages += "<div class='jira_issue'>" + issueId + " " + commit_id + "</div>";
                        }

                    }else{
                        incrementCommitCount("NonJIRACommitTotal");
                        nonJIRACommits++;
                        messages += "<div class='no_issue'>No Issue: " + commit_id + "</div>" ;
                    }
                }

                Integer nextCommitPage = pageNumber + 1;
                messages += this.syncCommits(nextCommitPage);

            }catch (JSONException e){
                //e.printStackTrace();
                messages = "GitHub Repository can't be found or incorrect credentials.";
            }

            return messages;

        }

        return "";

    }



    public String postReceiveHook(String payload){

        Date date = new Date();
        pluginSettingsFactory.createSettingsForKey(projectKey).put("githubLastSyncTime" + repositoryURL, date.toString());

        logger.debug("postBack()");
        String messages = "";

        try{
            JSONObject jsonCommits = new JSONObject(payload);
            JSONArray commits = jsonCommits.getJSONArray("commits");

            for (int i = 0; i < commits.length(); ++i) {
                String message = commits.getJSONObject(i).getString("message").toLowerCase();
                String commit_id = commits.getJSONObject(i).getString("id");

                // Detect presence of JIRA Issue Key
                if (message.indexOf(this.projectKey.toLowerCase()) > -1){

                        ArrayList extractedIssues = extractProjectKey(message);

                        for (int j=0; j < extractedIssues.size(); ++j){
                            String issueId = (String)extractedIssues.get(j).toString().toUpperCase();
                            addCommitID(issueId, commit_id, getBranchFromURL());
                            incrementCommitCount("JIRACommitTotal");

                            messages += "<div class='jira_issue'>" + issueId + " " + commit_id + "</div>";
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

    private String getRepositoryURLFromCommitURL(String commitURL){

        // Commit URL example
        // https://github.com/api/v2/json/commits/show/mojombo/grit/5071bf9fbfb81778c456d62e111440fdc776f76c?branch=master

        String[] arrayCommitURL = commitURL.split("/");
        String[] arrayBranch = commitURL.split("=");

        String branch = "";

        if(arrayBranch.length == 1){
            branch = "master";
        }else{
            branch = arrayBranch[1];
        }

        String repoBranchURL = "https://github.com/" + arrayCommitURL[8] + "/" + arrayCommitURL[9] + "/" + branch;
        logger.debug("GitHubCommits.getRepositoryURLFromCommitURL() - RepoBranchURL: " + repoBranchURL);
        return repoBranchURL;
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
                //logger.debug("Found commit id" + commitArray.get(i));
                boolExists = true;
            }
        }

        if (!boolExists){
            //logger.debug("addCommitID: Adding CommitID " + inferCommitDetailsURL() + commitId );
            commitArray.add(inferCommitDetailsURL() + commitId + "?branch=" + branch);
            addIssueId(issueId);
            pluginSettingsFactory.createSettingsForKey(projectKey).put("githubIssueCommitArray" + issueId, commitArray);
        }

    }


    // Removes a specific commit_id (URL) from the saved array
    private void removeCommitID(String issueId, String URLCommitID){
        ArrayList<String> commitArray = new ArrayList<String>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueCommitArray" + issueId) != null){
            commitArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueCommitArray" + issueId);
        }

        Boolean boolExists = false;
        ArrayList<String> newCommitArray = new ArrayList<String>();
        for (int i=0; i < commitArray.size(); i++){

            //logger.debug("GitHubCommits().removeCommitID - URLCommitID: " + URLCommitID);
            //logger.debug("GitHubCommits().removeCommitID - commitArray: " + commitArray.get(i));

            if (!URLCommitID.equals(commitArray.get(i))){
                newCommitArray.add(commitArray.get(i));
            }
        }

        pluginSettingsFactory.createSettingsForKey(projectKey).put("githubIssueCommitArray" + issueId, newCommitArray);

    }



    // Manages the recording of items ids for a JIRA project + Repository Pair so that we know
    // which issues within a project have commits associated with them
    private void addIssueId(String issueId){
        ArrayList<String> idsArray = new ArrayList<String>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueIDs" + repositoryURL) != null){
            idsArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueIDs" + repositoryURL);
        }

        Boolean boolExists = false;

        for (int i=0; i < idsArray.size(); i++){
            if ((issueId).equals(idsArray.get(i))){
                logger.debug("GitHubCommits.addIssueId() Found existing issue id - " + idsArray.get(i));
                boolExists = true;
            }
        }

        if (!boolExists){
            logger.debug("GitHubCommits.addIssueId() - " + issueId);
            idsArray.add(issueId);
            pluginSettingsFactory.createSettingsForKey(projectKey).put("githubIssueIDs" + repositoryURL, idsArray);
        }

    }

    // Removes all record of issues associated with this project and repository URL
    public void removeRepositoryIssueIDs(){

        ArrayList<String> idsArray = new ArrayList<String>();
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueIDs" + repositoryURL) != null){
            idsArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueIDs" + repositoryURL);
        }

        // Array of JIRA Issue IDS like ['PONE-4','PONE-10']
        for (int i=0; i < idsArray.size(); i++){
            //logger.debug("GitHubCommits.removeRepositoryIssueIDs() - " + idsArray.get(i));

            ArrayList<String> commitIDsArray = new ArrayList<String>();
            if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueCommitArray" + idsArray.get(i)) != null){
                commitIDsArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueCommitArray" + idsArray.get(i));

                // Array of Commit URL IDs like ['http://github.com/...']
                for (int j=0; j < commitIDsArray.size(); j++){
                    //logger.debug("GitHubCommits.removeRepositoryIssueIDs() - Commit ID: " + commitIDsArray.get(j));
                    //logger.debug("GitHubCommits.removeRepositoryIssueIDs() - " + getRepositoryURLFromCommitURL(commitIDsArray.get(j)));

                    if (repositoryURL.equals(getRepositoryURLFromCommitURL(commitIDsArray.get(j)))){
                        //logger.debug("match");
                        removeCommitID(idsArray.get(i), commitIDsArray.get(j));
                    }
                }
            }

        }

    }

}
