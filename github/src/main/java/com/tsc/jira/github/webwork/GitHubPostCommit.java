package com.tsc.jira.github.webwork;

import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: michaelbuckbee
 * Date: 4/14/11
 * Time: 4:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class GitHubPostCommit extends JiraWebActionSupport {

    final PluginSettingsFactory pluginSettingsFactory;

    public GitHubPostCommit(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void doValidation() {

        if (branch.equals("")){
            validations += "Missing Required GitHub 'branch' parameter. <br/>";
        }

        if (projectKey.equals("")){
            validations += "Missing Required 'projectKey' parameter. <br/>";
        }

        if (payload.equals("")){
            validations += "Missing Required GitHub 'payload' parameter. <br/>";
        }

    }

    protected String doExecute() throws Exception {

        if (validations.equals("")){
            System.out.println("Staring PostCommitUpdate");

            JSONObject jsonPayload = new JSONObject(payload);
            JSONObject jsonRepository = jsonPayload.getJSONObject("repository");

            String baseRepositoryURL = jsonRepository.getString("url");
            String url = baseRepositoryURL + "/" + branch;

            GitHubCommits repositoryCommits = new GitHubCommits(pluginSettingsFactory);
            repositoryCommits.repositoryURL = url;
            repositoryCommits.projectKey = projectKey;

            // Starts actual search of commits via GitAPI, "1" is the first
            // page of commits to be returned via the API
            validations = repositoryCommits.postReceiveHook(payload);

        }

        return "postcommit";
    }

    // Validation Error Messages
    private String validations = "";
    public String getValidations(){return this.validations;}

    // GitHub JSON Payload
    private String payload = "";
    public void setPayload(String value){this.payload = value;}
    public String getPayload(){return payload;}

    // Project Key
    private String projectKey = "";
    public void setProjectKey(String value){this.projectKey = value;}
    public String getProjectKey(){return projectKey;}

    // GitHub Repository URL
    private String branch = "";
    public void setBranch(String value){this.branch = value;}
    public String getBranch(){return branch;}

}
