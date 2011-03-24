package com.tsc.jira.github.webwork;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

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
    }

    protected String doExecute() throws Exception {
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
    }


    public String getSavedClientSecret(){
        return (String)pluginSettingsFactory.createGlobalSettings().get("githubRepositoryClientSecret");
    }

    public String getSavedClientID(){
        return (String)pluginSettingsFactory.createGlobalSettings().get("githubRepositoryClientID");
    }

    // Validation Error Messages
    private String validations = "";
    public String getValidations(){return this.validations;}

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