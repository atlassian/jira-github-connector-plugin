package com.tsc.jira.github.webwork;

import com.atlassian.jira.project.Project;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class GitHubOAuth2 extends JiraWebActionSupport {

    final PluginSettingsFactory pluginSettingsFactory;

    public GitHubOAuth2(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void doValidation() {

    }

    protected String doExecute() throws Exception {

        if(code.length() > 0){
                projectKey = (String)pluginSettingsFactory.createGlobalSettings().get("githubPendingProjectKey");
                privateRepositoryURL = (String)pluginSettingsFactory.createGlobalSettings().get("githubPendingRepositoryURL");

                // strips "access_token=" from result returned from GitHub
                access_token = requestAccessToken().split("=")[1];
                pluginSettingsFactory.createSettingsForKey(projectKey).put("githubRepositoryAccessToken" + privateRepositoryURL, access_token);
        }


        return "success";
    }

    private String requestAccessToken(){

        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try {

            String clientID = (String)pluginSettingsFactory.createGlobalSettings().get("githubRepositoryClientID");
            String clientSecret = (String)pluginSettingsFactory.createGlobalSettings().get("githubRepositoryClientSecret");

            System.out.println("requestAccessToken() - " + "https://github.com/login/oauth/access_token?&client_id=" + clientID + "&client_secret=" + clientSecret + "&code=" + code);

            url = new URL("https://github.com/login/oauth/access_token?&client_id=" + clientID + "&client_secret=" + clientSecret + "&code=" + code);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("POST");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                System.out.println("RESPONSE: " + line);
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



    // GitHub Temporary Code
    private String code;
    public void setCode(String value){this.code = value;}
    public String getCode(){return code;}

    // GitHub Access Token
    private String access_token;
    public String getAccess_token(){return access_token;}

    // Project Key
    private String projectKey = "";
    public String getProjectKey(){return projectKey;}

    // Private Repository URL
    private String privateRepositoryURL = "";
    public String getPrivateRepositoryURL(){return privateRepositoryURL;}

    // Form Directive
    private String nextAction = "";
    public void setNextAction(String value){this.nextAction = value;}
    public String getNextAction(){return this.nextAction;}

    // Validation Error Messages
    private String validations = "";
    public String getValidations(){return this.validations;}




}
