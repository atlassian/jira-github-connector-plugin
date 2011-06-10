package com.atlassian.jira.plugins.github.webwork;

import com.atlassian.jira.config.properties.PropertiesManager;
import com.atlassian.jira.project.Project;

import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHubOAuth2 extends JiraWebActionSupport {

    final PluginSettingsFactory pluginSettingsFactory;
    final Logger logger = LoggerFactory.getLogger(GitHubOAuth2.class);

    public GitHubOAuth2(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void doValidation() {

        if(error != null){
            if(error.equals("user_denied")){
                validations = "user_denied";
            }
        }

    }


    protected String doExecute() throws Exception {

        if(validations.equals("user_denied")){
            return "cancelled";
        }else{

            if(code.length() > 0){
                    projectKey = (String)pluginSettingsFactory.createGlobalSettings().get("githubPendingProjectKey");
                    privateRepositoryURL = (String)pluginSettingsFactory.createGlobalSettings().get("githubPendingRepositoryURL");

                    // strips "access_token=" from result returned from GitHub
                    access_token = requestAccessToken().split("=")[1];

                    if (access_token.equals("incorrect_client_credentials") || access_token.equals("bad_verification_code")){

                        return "error";

                    }else{

                    // Verification Success
                        pluginSettingsFactory.createSettingsForKey(projectKey).put("githubRepositoryAccessToken" + privateRepositoryURL, access_token);

                        String[] urlArray = privateRepositoryURL.split("/");
                        postCommitURL = "GitHubPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length-1];
                    }
            }

            return "success";
        }

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

            logger.debug("requestAccessToken() - " + "https://github.com/login/oauth/access_token?&client_id=" + clientID + "&client_secret=" + clientSecret + "&code=" + code);

            url = new URL("https://github.com/login/oauth/access_token?&client_id=" + clientID + "&client_secret=" + clientSecret + "&code=" + code);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("POST");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                logger.debug("RESPONSE: " + line);
                result += line;
            }
            rd.close();
        }catch (MalformedURLException e){
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        result = result.replace("&token_type","");

        return result;

    }



    // GitHub Temporary Code
    private String code;
    public void setCode(String value){this.code = value;}
    public String getCode(){return code;}

    // GitHub Access Token
    private String access_token;
    public String getAccess_token(){return access_token;}

    // GitHub OAuth2 Error message parameter
    private String error;
    public void setError(String value){this.error = value;}
    public String getError(){return error;}

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

    // GitHub Post Commit URL for a specific project and repository
    private String postCommitURL = "";
    public void setPostCommitURL(String value){this.postCommitURL = value;}
    public String getPostCommitURL(){return postCommitURL;}

    // Base URL
    private String baseURL = PropertiesManager.getInstance().getPropertySet().getString("jira.baseurl");
    public String getBaseURL(){return this.baseURL;}

}
