package com.atlassian.jira.plugins.github.webwork;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.project.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewProjectRepository extends JiraWebActionSupport{

    final PluginSettingsFactory pluginSettingsFactory;
    final Logger logger = LoggerFactory.getLogger(ViewProjectRepository.class);

    public ViewProjectRepository(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void doValidation() {
        for (Enumeration e =  request.getParameterNames(); e.hasMoreElements() ;) {
            String n = (String)e.nextElement();
            String[] vals = request.getParameterValues(n);
        }
    }

    protected String doExecute() throws Exception {
        return "viewrepository";
    }

    // GitHub Repository URL
    private String url = "";
    public void setUrl(String value){this.url = value;}
    public String getURL(){return url;}

    // Project Key
    private String projectKey = "";
    public void setProjectKey(String value){this.projectKey = value;}
    public String getProjectKey(){return projectKey;}

    // Project Name
    private String projectName = "";
    public void setProjectName(String value){this.projectName = value;}
    public String getProjectName(){return projectName;}

    // JIRA Project Listing
    private ComponentManager cm = ComponentManager.getInstance();
    private Project project = cm.getProjectManager().getProjectObjByKey(projectKey);


}
