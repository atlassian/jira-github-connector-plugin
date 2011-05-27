package com.atlassian.jira.plugins.github.links;

import com.atlassian.jira.config.properties.PropertiesManager;
import com.atlassian.jira.plugin.projectoperation.AbstractPluggableProjectOperation;
import com.atlassian.jira.project.Project;

import com.opensymphony.user.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectSettings extends AbstractPluggableProjectOperation{

    final Logger logger = LoggerFactory.getLogger(ProjectSettings.class);

   public String getHtml(final Project project, final User user){

       String baseURL = PropertiesManager.getInstance().getPropertySet().getString("jira.baseurl");

       return "<strong>GitHub Connector: </strong> (<a href='" + baseURL + "/secure/admin/GitHubConfigureRepositories.jspa?projectKey=" + project.getKey() + "&mode=single'>Manage Repositories</a>)";

   }

   public boolean showOperation(final Project project, final User user)
   {
       return true;
   }
}
