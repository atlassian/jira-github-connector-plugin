package com.tsc.jira.github.webwork;

import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
        return "https://github.com/api/v2/json/commits/show/" + path[3] + "/" + path[4] +"/" + path[5];
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
            e.printStackTrace();
        }

        return result;
    }

    // Commit list returns id (hashed) and Message
    // you have to call each individual commit to get diff details
    private String getCommitDetails(String commit_id){
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(this.inferCommitDetailsURL() + commit_id);
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

    public String searchCommits(Integer pageNumber){
        System.out.println("searchCommits()");
        String commitsAsJSON = getCommitsList(pageNumber);

        String messages = "";

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
                            messages += "<div class='jira_issue'>" + extractProjectKey(message) + " " + commit_id + "</div>";
                            String commitDetailsJSON = getCommitDetails(commit_id);
                        }

                    }else{
                        messages += "<div class='no_issue'>No Issue: " + commit_id + "</div>" ;
                    }
                }

                messages += this.searchCommits(pageNumber + 1);

            }catch (JSONException e){
                e.printStackTrace();
                return "exception";
            }

            return messages;

        }

        return "";

    }


}
