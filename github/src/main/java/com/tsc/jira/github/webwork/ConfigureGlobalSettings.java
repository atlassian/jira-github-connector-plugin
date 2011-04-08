package com.tsc.jira.github.webwork;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import com.atlassian.jira.user.util.UserManager;
import com.atlassian.crowd.embedded.api.User;

public class ConfigureGlobalSettings extends JiraWebActionSupport {

    final PluginSettingsFactory pluginSettingsFactory;

    public ConfigureGlobalSettings(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void doValidation() {
        if(nextAction.equals("SetOAuthValues")){
            if(clientSecret.equals("") || clientID.equals("")){
                validations = "Please enter both the GitHub OAuth Client ID and Client Secret";
            }
        }

        if(nextAction.equals("SetJiraGitHubUser")){

            User githubUser = ComponentManager.getInstance().getUserUtil().getUserObject(jiraGitHubUser);

            if(githubUser == null){
                validations = "Please specify an existing JIRA user for your GitHub actions.";
            }

        }

    }

    protected String doExecute() throws Exception {

        if(nextAction.equals("SetJiraGitHubUser")){
            if(validations.equals("")){
                  pluginSettingsFactory.createGlobalSettings().put("githubJiraGitHubUser", jiraGitHubUser);
            }
        }

        if(nextAction.equals("SetOAuthValues")){
            if(validations.equals("")){
                addClientIdentifiers();
            }
        }

        return "input";
    }

    private void addClientIdentifiers(){
        pluginSettingsFactory.createGlobalSettings().put("githubRepositoryClientID", clientID);
        pluginSettingsFactory.createGlobalSettings().put("githubRepositoryClientSecret", clientSecret);

        messages = "GitHub Client Identifiers Set Correctly";

    }


    public String getSavedClientSecret(){

        String savedClientSecret = (String)pluginSettingsFactory.createGlobalSettings().get("githubRepositoryClientSecret");

        if(savedClientSecret == null){
            return "";
        }else{
            return savedClientSecret;
        }
    }

    public String getSavedClientID(){

        String savedClientID = (String)pluginSettingsFactory.createGlobalSettings().get("githubRepositoryClientID");

        if(savedClientID == null){
            return "";
        }else{
            return savedClientID;
        }
    }


    public String getSavedJiraGitHubUser(){
        String jiraGitHubUser = (String)pluginSettingsFactory.createGlobalSettings().get("githubJiraGitHubUser");

        if(jiraGitHubUser == null){
            return "";
        }else{
            return jiraGitHubUser;
        }
    }

    // Validation Error Messages
    private String validations = "";
    public String getValidations(){return this.validations;}

    // JIRA GitHub User
    private String jiraGitHubUser = "";
    public void setJiraGitHubUser(String value){this.jiraGitHubUser = value;}
    public String getJiraGitHubUser(){return this.jiraGitHubUser;}

    // Client ID (from GitHub OAuth Application)
    private String clientID = "";
    public void setClientID(String value){this.clientID = value;}
    public String getClientID(){return this.clientID;}

    // Client Secret (from GitHub OAuth Application)
    private String clientSecret = "";
    public void setClientSecret(String value){this.clientSecret = value;}
    public String getClientSecret(){return this.clientSecret;}

    // Form Directive
    private String nextAction = "";
    public void setNextAction(String value){this.nextAction = value;}
    public String getNextAction(){return this.nextAction;}

    // Confirmation Messages
    private String messages = "";
    public String getMessages(){return this.messages;}

}