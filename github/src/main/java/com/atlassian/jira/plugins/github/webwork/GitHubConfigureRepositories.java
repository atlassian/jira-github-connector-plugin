package com.atlassian.jira.plugins.github.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.config.properties.PropertiesManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubConfigureRepositories extends JiraWebActionSupport {

    final PluginSettingsFactory pluginSettingsFactory;

    public GitHubConfigureRepositories(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void doValidation() {
        //System.out.println("GitHubConfigureRepositories - doValidation()");
        for (Enumeration e =  request.getParameterNames(); e.hasMoreElements() ;) {
            String n = (String)e.nextElement();
            String[] vals = request.getParameterValues(n);
            //validations = validations + "name " + n + ": " + vals[0];
        }

        // GitHub URL Validation
        if (!url.equals("")){
            System.out.println("URL for Evaluation: " + url + " - NA: " + nextAction);
            if (nextAction.equals("AddRepository") || nextAction.equals("DeleteReposiory")){
                // Valid URL and URL starts with github.com domain
                Pattern p = Pattern.compile("^(https|http)://github.com/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
                Matcher m = p.matcher(url);
                if (!m.matches()){
                    validations = "URL must be for a valid GitHub.com repository.";
                }
            }
        }else{
                validations = "Please supply a valid GitHub repository.";
        }

    }

    protected String doExecute() throws Exception {
        System.out.println("NextAction: " + nextAction);

        // Remove trailing slashes from URL
        if (url.endsWith("/")){
            url = url.substring(0, url.length() - 1);
        }

        // Set all URLs to HTTPS
        if (url.startsWith("http:")){
            url = url.replaceFirst("http:","https:");
        }

        // Add default branch of 'master' to URL if missing
        String[] urlArray = url.split("/");

        if(urlArray.length == 5){
            url += "/master";
        }

        if (validations.equals("")){
            if (nextAction.equals("AddRepository")){

                if (repoVisibility.equals("private")){
                    System.out.println("Private Add Repository");
                    String clientID = (String)pluginSettingsFactory.createGlobalSettings().get("githubRepositoryClientID");

                    if(clientID.equals(null) || clientID.equals("")){
                        //System.out.println("No Client ID");
                        validations = "You will need to setup a <a href='/secure/admin/ConfigureGlobalSettings.jspa'>GitHub OAuth Application</a> before you can add private repositories";
                    }else{
                        addRepositoryURL();
                        pluginSettingsFactory.createGlobalSettings().put("githubPendingProjectKey", projectKey);
                        pluginSettingsFactory.createGlobalSettings().put("githubPendingRepositoryURL", url);

                        String redirectURI = "https://github.com/login/oauth/authorize?scope=repo&client_id=" + clientID;
                        redirectURL = redirectURI;

                        return "redirect";
                    }
                }else{
                    System.out.println("PUBLIC Add Repository");
                    addRepositoryURL();
                    syncRepository();
                }

                postCommitURL = "GitHubPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length-1];

                System.out.println(postCommitURL);

            }

            if (nextAction.equals("DeleteRepository")){
                deleteRepositoryURL();
            }

            if (nextAction.equals("SyncRepository")){
                syncRepository();

            }
        }

        return INPUT;
    }

    private void syncRepository(){
        System.out.println("Staring Repository Sync");

        GitHubCommits repositoryCommits = new GitHubCommits(pluginSettingsFactory);
        repositoryCommits.repositoryURL = url;
        repositoryCommits.projectKey = projectKey;

        // Reset Commit count
        pluginSettingsFactory.createSettingsForKey(projectKey).put("NonJIRACommitTotal" + url, null);
        pluginSettingsFactory.createSettingsForKey(projectKey).put("JIRACommitTotal" + url, null);

        // Starts actual search of commits via GitAPI, "1" is the first
        // page of commits to be returned via the API
        messages = repositoryCommits.syncCommits(1);

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

        // Remove associated access key (if any) for private repos
        pluginSettingsFactory.createSettingsForKey(projectKey).remove("githubRepositoryAccessToken" + url);

        urlArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubRepositoryURLArray");

        for (int i=0; i < urlArray.size(); i++){
            if (url.equals(urlArray.get(i))){
                urlArray.remove(i);

                GitHubCommits repositoryCommits = new GitHubCommits(pluginSettingsFactory);
                repositoryCommits.repositoryURL = url;
                repositoryCommits.projectKey = projectKey;

                repositoryCommits.removeRepositoryIssueIDs();

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

    // GitHub Post Commit URL for a specific project and repository
    private String postCommitURL = "";
    public void setPostCommitURL(String value){this.postCommitURL = value;}
    public String getPostCommitURL(){return postCommitURL;}

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
