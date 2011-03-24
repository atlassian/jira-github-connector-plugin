package com.tsc.jira.github.webwork;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.config.properties.PropertiesManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigureRepositories extends JiraWebActionSupport {

    final PluginSettingsFactory pluginSettingsFactory;

    public ConfigureRepositories(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void doValidation() {

        for (Enumeration e =  request.getParameterNames(); e.hasMoreElements() ;) {
            String n = (String)e.nextElement();
            String[] vals = request.getParameterValues(n);
            //validations = validations + "name " + n + ": " + vals[0];
        }

        // GitHub URL Validation
        if (url.equals("") && (nextAction.equals("addRepository") || nextAction.equals("deleteReposiory"))) {
            // Valid URL and URL starts with github.com domain
            Pattern p = Pattern.compile("^(https|http)://github.com/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
            Matcher m = p.matcher(url);
            if (!m.matches()){
                addErrorMessage("URL must be for a valid GitHub.com repository.");
                validations = "URL must be for a valid GitHub.com repository.";
            }
        }
    }

    protected String doExecute() throws Exception {

        if (validations.equals("")){
            if (nextAction.equals("AddRepository")){

                if (repoVisibility.equals("private")){
                    System.out.println("Private Add Repository");
                    String clientID = (String)pluginSettingsFactory.createGlobalSettings().get("githubRepositoryClientID");

                    if(clientID.equals("")){
  //                      System.out.println("No Client ID");
                        validations = "You will need to setup a new <a href='/jira/secure/admin/ConfigureGlobalSettings.jspa'>GitHub OAuth Application</a> before you can add private repositories";
                    }else{
                        addRepositoryURL();
//                        System.out.println("Add Private Repository URL");

                        pluginSettingsFactory.createGlobalSettings().put("githubPendingProjectKey", projectKey);
                        pluginSettingsFactory.createGlobalSettings().put("githubPendingRepositoryURL", url);

                        // ToDo: Switch to production (JIRA) URL
                        //String redirectURI = "http://github.com/login/oauth/authorize?client_id=" + clientID + "&redirect_uri=$action.getBaseURL()/secure/admin/GitHubOAuth2";
                        String redirectURI = "http://github.com/login/oauth/authorize?client_id=" + clientID + "&redirect_uri=http://www.flickscanapp.com/rails/movies/upc";

                        // ToDo: Server side redirect
                        this.forceRedirect(redirectURI);

                        redirectURL = redirectURI;

                        return "redirect";
                    }
                }else{
                    System.out.println("PUBLIC Add Repository");
                    //addRepositoryURL();
                }
            }

            if (nextAction.equals("DeleteRepository")){
                deleteRepositoryURL();
            }

            if (nextAction.equals("SyncRepository")){

                Date date = new Date();
                pluginSettingsFactory.createSettingsForKey(projectKey).put("githubLastSyncTime", date.toString());

                GitHubCommits repositoryCommits = new GitHubCommits();
                repositoryCommits.repositoryURL = url;
                repositoryCommits.projectKey = projectKey;

                // Starts actual search of commits via GitAPI, "1" represents the first
                // page of commits to be returned via the API
                messages = repositoryCommits.searchCommits(1);

            }
        }

        return INPUT;
    }

    // Manages the entry of multiple repository URLs in a single pluginSetting Key
    private void addRepositoryURL(){
        ArrayList<String> urlArray = new ArrayList<String>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubRepositoryURLArray") != null){
            urlArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubRepositoryURLArray");
        }

        Boolean boolExists = false;

        for (int i=0; i < urlArray.size(); i++){
            if (url.equals(urlArray.get(i))){
                boolExists = true;
            }
        }

        if (!boolExists){
            urlArray.add(url);
            pluginSettingsFactory.createSettingsForKey(projectKey).put("githubRepositoryURLArray", urlArray);
        }

    }

    // Removes a single Repository URL from a given Project
    private void deleteRepositoryURL(){
        ArrayList<String> urlArray = new ArrayList<String>();

        urlArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubRepositoryURLArray");

        for (int i=0; i < urlArray.size(); i++){
            if (url.equals(urlArray.get(i))){
                urlArray.remove(i);
            }
        }

        pluginSettingsFactory.createSettingsForKey(projectKey).put("githubRepositoryURLArray", urlArray);

    }

    // JIRA Project Listing
    private ComponentManager cm = ComponentManager.getInstance();
    private List<Project> projects = cm.getProjectManager().getProjectObjects();

    public List getProjects(){
        return projects;
    }


    // Stored Repository + JIRA Projects
    public ArrayList<String> getProjectRepositories(String pKey){
        return (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(pKey).get("githubRepositoryURLArray");
    }

    // GitHub Repository URL
    private String url = "";
    public void setUrl(String value){this.url = value;}
    public String getURL(){return url;}

    // GitHub Repository Visibility
    private String repoVisibility = "";
    public void setRepoVisibility(String value){this.repoVisibility = value;}
    public String getRepoVisibility(){return repoVisibility;}

    // Project Key
    private String projectKey = "";
    public void setProjectKey(String value){this.projectKey = value;}
    public String getProjectKey(){return projectKey;}

    // Form Directive
    private String nextAction = "";
    public void setNextAction(String value){this.nextAction = value;}
    public String getNextAction(){return this.nextAction;}

    // Validation Error Messages
    private String validations = "";
    public String getValidations(){return this.validations;}

    // Confirmation Messages
    private String messages = "";
    public String getMessages(){return this.messages;}

    // Base URL
    private String baseURL = PropertiesManager.getInstance().getPropertySet().getString("jira.baseurl");
    public String getBaseURL(){return this.baseURL;}

    // Redirect URL
    private String redirectURL = "";
    public String getRedirectURL(){return this.redirectURL;}

}
