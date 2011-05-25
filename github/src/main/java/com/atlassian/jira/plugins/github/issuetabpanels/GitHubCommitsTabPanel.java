package com.atlassian.jira.plugins.github.issuetabpanels;

import com.atlassian.core.util.StringUtils;
import com.atlassian.core.util.collection.EasyList;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.opensymphony.user.User;
import com.atlassian.jira.plugins.github.webwork.GitHubCommits;

import com.atlassian.jira.web.action.JiraWebActionSupport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;



public class GitHubCommitsTabPanel extends AbstractIssueTabPanel {

    final PluginSettingsFactory pluginSettingsFactory;

    public String repositoryURL;
    public String repoLogin;
    public String repoName;
    public String branch;

    public GitHubCommitsTabPanel(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void populateVelocityParams(Map params)
    {
        params.put("stringUtils", new StringUtils());
        params.put("github", this);
    }

    private String getRepositoryURLFromCommitURL(String commitURL){

        // https://github.com/api/v2/json/commits/show/mojombo/grit/5071bf9fbfb81778c456d62e111440fdc776f76c?branch=master

        String[] arrayCommitURL = commitURL.split("/");
        String[] arrayBranch = commitURL.split("=");

        String branch = "";

        if(arrayBranch.length == 1){
            branch = "master";
        }else{
            branch = arrayBranch[1];
        }

        String repoBranchURL = "https://github.com/" + arrayCommitURL[8] + "/" + arrayCommitURL[9] + "/" + branch;
        System.out.println("RepoBranchURL: " + repoBranchURL);

        this.repositoryURL = repoBranchURL;
        this.repoLogin = arrayCommitURL[8];
        this.repoName = arrayCommitURL[9];
        this.branch = branch;

        return repoBranchURL;
    }

    public List getActions(Issue issue, User user) {
        String projectKey = issue.getProjectObject().getKey();
        String issueId = (String)issue.getKey();

        GitHubCommits gitHubCommits = new GitHubCommits(pluginSettingsFactory);
        gitHubCommits.projectKey = projectKey;

        ArrayList<String> commitArray = new ArrayList<String>();

        String issueCommitActions = "No GitHub Commits Found";

        ArrayList<Object> githubActions = new ArrayList<Object>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueCommitArray" + issueId) != null){
            commitArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueCommitArray" + issueId);

            for (int i=0; i < commitArray.size(); i++){
                    System.out.println("Found commit id" + commitArray.get(i));

                    gitHubCommits.repositoryURL = getRepositoryURLFromCommitURL(commitArray.get(i));
                    String commitDetails = gitHubCommits.getCommitDetails(commitArray.get(i));

                    issueCommitActions = this.formatCommitDetails(commitDetails);
                    GenericMessageAction action = new GenericMessageAction(issueCommitActions);
                    githubActions.add(action);

                    System.out.println("Commit Entry: " + "githubIssueCommitArray" + i );

            }

        }

        return EasyList.build(githubActions);


    }

    public boolean showPanel(Issue issue, User user) {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private Date parseISO8601(String input) throws ParseException{
        //NOTE: SimpleDateFormat uses GMT[-+]hh:mm for the TZ which breaks
        //things a bit.  Before we go on we have to repair this.
        SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz" );

        //this is zero time so we need to add that TZ indicator for
        if ( input.endsWith( "Z" ) ) {
            input = input.substring( 0, input.length() - 1) + "GMT-00:00";
        } else {
            int inset = 6;

            String s0 = input.substring( 0, input.length() - inset );
            String s1 = input.substring( input.length() - inset, input.length() );

            input = s0 + "GMT" + s1;
        }

        return df.parse(input);

    }

    private String formatCommitDate(Date commitDate) throws ParseException{
        SimpleDateFormat sdfGithub = new SimpleDateFormat("MMM d yyyy KK:mm:ss");
        return sdfGithub.format(commitDate);
    }


    private String extractDiffInformation(String diff){

        if (!diff.trim().equals("")){
            // the +3 and -1 remove the leading and trailing spaces

            //System.out.println("Diff STring: " + diff);

            Integer first = diff.indexOf("@@") + 3;
            Integer second = diff.indexOf("@@", first) -1;

            //System.out.println("first: " + first.toString());
            //System.out.println("second: " + second.toString());

            String[] modLine = diff.substring(first,second).replace("+","").replace("-","").split(" ");

            String[] removedEntryArray = modLine[0].split(",");
            String[] addedEntryArray = modLine[1].split(",");

            String removedEntry = "";
            String addedEntry = "";

            if (removedEntryArray.length == 1){
                removedEntry = removedEntryArray[0];
            }else{
                removedEntry = removedEntryArray[1];
            }

            if (addedEntryArray.length == 1){
                addedEntry = addedEntryArray[0];
            }else{
                addedEntry = addedEntryArray[1];
            }

            if (addedEntry.trim().equals("0")){
                addedEntry = "<span style='color: gray'>+" + addedEntry + "</span>";
            }else{
                addedEntry = "<span style='color: green'>+" + addedEntry + "</span>";
            }

            if (removedEntry.trim().equals("0")){
                removedEntry = "<span style='color: gray'>-" + removedEntry + "</span>";
            }else{
                removedEntry = "<span style='color: red'>-" + removedEntry + "</span>";
            }


            return addedEntry + " " + removedEntry;
        }else{
            return "<span style='color: gray'>+0 -0</span>";
        }
    }

    private String fileCommitURL(String filename, String commitHash){
        // https://github.com/mbuckbee/projecttest/blob/118f75ca466da85525b79bf9d8836aae64b5f949/file1
        String fileCommitURL = "https://github.com/" + repoLogin + "/" + repoName + "/blob/" + commitHash + "/" + filename;
        return fileCommitURL;

    }

    private String formatCommitDetails(String jsonDetails){
            try{

                JiraWebActionSupport jwas = new JiraWebActionSupport();

                jwas.htmlEncode("heyo");

                JSONObject jsonCommits = new JSONObject(jsonDetails);
                JSONObject commit = jsonCommits.getJSONObject("commit");

                String message = commit.getString("message");
                String commit_hash = commit.getString("id");

                JSONObject author = commit.getJSONObject("author");
                String authorName = author.getString("name");
                String login = author.getString("login");
                String commitURL = commit.getString("url");

                String[] commitURLArray = commitURL.split("/");

                String projectName = commitURLArray[2];

                String committedDateString = commit.getString("committed_date");

                String formattedCommitDate = "";

                try{
                    Date committedDate = parseISO8601(committedDateString);
                    formattedCommitDate = formatCommitDate(committedDate);

                }catch (ParseException pe){

                }

                String commitTree = commit.getString("tree");
                String commitMessage = commit.getString("message");
                JSONObject githubUser = new JSONObject(getUserDetails(login));

                JSONObject user = githubUser.getJSONObject("user");
                String userName = "";
                try{
                    userName = user.getString("name");
                }catch (JSONException je){
                     System.out.println("Missing Username");
                }

                String gravatarHash = user.getString("gravatar_id");
                String gravatarUrl = "http://www.gravatar.com/avatar/" + gravatarHash + "?s=60";

                String htmlParentHashes = "";

                if(commit.has("parents")){
                    JSONArray arrayParents = commit.getJSONArray("parents");

                    for (int i=0; i < arrayParents.length(); i++){
                        String parentHashID = arrayParents.getJSONObject(i).getString("id");
                        htmlParentHashes = "<tr><td style='color: #757575'>Parent:</td><td><a href='" + "https://github.com/" + jwas.htmlEncode(login) + "/" + jwas.htmlEncode(projectName) + "/commit/" + parentHashID +"' target='_new'>" + parentHashID + "</a></td></tr>";
                    }

                }

                Map mapFiles = Collections.synchronizedMap(new TreeMap());

                String htmlAdded = "";

                if(commit.has("added")){
                    JSONArray arrayAdded = commit.getJSONArray("added");

                    for (int i=0; i < arrayAdded.length(); i++){
                          String addFilename = jwas.htmlEncode(arrayAdded.getString(i));
                          htmlAdded = "<li><span style='color:green; font-size: 8pt;'>ADDED</span>  <a href='" + fileCommitURL(addFilename, commit_hash) + "' target='_new'>" + addFilename + "</a></li>";
                          mapFiles.put(addFilename, htmlAdded);

                    }



                }

                String htmlRemoved = "";

                if(commit.has("removed")){
                    JSONArray arrayRemoved = commit.getJSONArray("removed");

                    for (int i=0; i < arrayRemoved.length(); i++){
                          String removeFilename = jwas.htmlEncode(arrayRemoved.getString(i));
                          htmlRemoved = "<li><span style='color:red; font-size: 8pt;'>DELETED</span>  <a href='" + fileCommitURL(removeFilename, commit_hash) + "' target='_new'>" + removeFilename + "</a></li>";
                          mapFiles.put(removeFilename, htmlRemoved);
                    }

                }

                String htmlModified = "";

                if(commit.has("modified")){
                    JSONArray arrayModified = commit.getJSONArray("modified");

                    for (int i=0; i < arrayModified.length(); i++){
                          String modFilename = jwas.htmlEncode(arrayModified.getJSONObject(i).getString("filename"));
                          String modDiff = arrayModified.getJSONObject(i).getString("diff");
                          htmlModified = "<li><span font-size: 8pt;'>" + extractDiffInformation(modDiff) + "</span>  <a href='" + fileCommitURL(modFilename, commit_hash) + "' target='_new'>"+ modFilename + "</a></li>";

                          mapFiles.put(modFilename, htmlModified);

                    }

                }

                String htmlFiles = "";
                String htmlFilesHiddenDescription = "";
                Integer numSeeMore = 0;
                Random randDivID = new Random(System.currentTimeMillis());

                // Sort and compose all files
                Iterator it = mapFiles.keySet().iterator();
                Object obj;

                String htmlHiddenDiv = "";

                if(mapFiles.size() <= 5){
                    while (it.hasNext()) {
                      obj = it.next();
                      htmlFiles += mapFiles.get(obj);
                    }

                    htmlFilesHiddenDescription = "";

                }else{

                    Integer i = 0;

                    while (it.hasNext()) {
                        obj = it.next();

                        if(i <= 4){
                            htmlFiles += mapFiles.get(obj);
                        }else{
                            htmlHiddenDiv += mapFiles.get(obj);
                        }

                        i++;
                    }

                    numSeeMore = mapFiles.size() - 5;
                    Integer divID = randDivID.nextInt();

                    htmlFilesHiddenDescription = "<div class='see_more' target_div='" + divID.toString() + "' id='see_more_" + divID.toString() + "' style='color: #3C78B5'>" +
                                "See " + numSeeMore.toString() + " more" +
                            "</div>" +
                            "<div class='hide_more' id='hide_more_" + divID.toString() + "' target_div='" + divID.toString() + "' style='display: none; color: #3C78B5'>Hide " + numSeeMore.toString() + " Files</div>";

                    htmlHiddenDiv = htmlFilesHiddenDescription + "<div id='" + divID.toString() + "' style='display: none;'><ul>" + htmlHiddenDiv + "</ul></div>";

                }




String htmlCommitEntry = "" +
    "<table>" +
        "<tr>" +
            "<td valign='top' width='70px'><a href='#user_url' target='_new'><img src='#gravatar_url' border='0'></a></td>" +
            "<td valign='top'>" +
                "<div style='padding-bottom: 6px'><a href='#user_url' target='_new'>#user_name - #login</a></div>" +
                "<table>" +
                    "<tr>" +
                        "<td>" +
                            "<div style='border-left: 2px solid #C9D9EF; background-color: #EAF3FF; color: #5D5F62; padding: 5px; margin-bottom: 10px;'>#commit_message</div>" +

                                "<ul>" +
                                    htmlFiles +
                                "</ul>" +

                                htmlHiddenDiv +

                            "<div style='margin-top: 10px'>" +
                                "<img src='/download/resources/com.atlassian.jira.plugins.github.GitHub/images/document.jpg' align='center'> <span class='commit_date' style='color: #757575; font-size: 9pt;'>#formatted_commit_date</span>" +
                            "</div>" +

                        "</td>" +

                        "<td width='400' style='padding-top: 0px' valign='top'>" +
                            "<div style='border-left: 2px solid #cccccc; margin-left: 15px; margin-top: 0px; padding-top: 0px; padding-left: 10px'>" +
                                "<table style='margin-top: 0px; padding-top: 0px;'>" +
                                    "<tr><td style='color: #757575'>Commit:</td><td><a href='#commit_url' target='_new'>#commit_hash</a></td></tr>" +
                                     "<tr><td style='color: #757575'>Tree:</td><td><a href='#tree_url' target='_new'>#tree_hash</a></td></tr>" +
                                     htmlParentHashes +
                                "</table>" +
                            "</div>" +
                        "</td>" +

                    "</tr>" +
                "</table>" +
        "</td>" +
    "</tr>" +
"</table>";


                htmlCommitEntry = htmlCommitEntry.replace("#gravatar_url", gravatarUrl);
                htmlCommitEntry = htmlCommitEntry.replace("#user_url", "https://github.com/" + jwas.htmlEncode(login));
                htmlCommitEntry = htmlCommitEntry.replace("#login", jwas.htmlEncode(login));

                htmlCommitEntry = htmlCommitEntry.replace("#user_name", jwas.htmlEncode(userName));

                htmlCommitEntry = htmlCommitEntry.replace("#commit_message", jwas.htmlEncode(commitMessage));

                htmlCommitEntry = htmlCommitEntry.replace("#formatted_commit_time", committedDateString);




                htmlCommitEntry = htmlCommitEntry.replace("#formatted_commit_date", formattedCommitDate);

                htmlCommitEntry = htmlCommitEntry.replace("#commit_url", "https://github.com" + commitURL);
                htmlCommitEntry = htmlCommitEntry.replace("#commit_hash", commit_hash);

                htmlCommitEntry = htmlCommitEntry.replace("#tree_url", "https://github.com/" + login + "/" + projectName + "/tree/" + commit_hash);

                htmlCommitEntry = htmlCommitEntry.replace("#tree_hash", commitTree);
                return htmlCommitEntry;

             // Catches invalid or removed GitHub IDs, but errors are suppressed as they typically
            }catch (JSONException e){
                e.printStackTrace();
                return "Information can't be retrieved from GitHub. Please contact your administrator.";
            }

    }

    private String getUserDetails(String loginName){

        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL("http://github.com/api/v2/json/user/show/" + loginName);
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


}
