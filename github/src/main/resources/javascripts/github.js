

function openJIRAStudioGitHubNavigation(menu_item){
        AJS.$('#menu_section_github').parent().addClass('studio-admin-menu-section-selected');
        AJS.$('#menu_section_github').parent().children('ul').css('display','block');
        AJS.$('#menu_section_github').parent().children('a').first().addClass('studio-admin-menu-section-hide').removeClass('studio-admin-menu-section-show');
        AJS.$('#' + menu_item).parent().addClass('studio-admin-menu-item-selected');
}


window.onload = function(){
    if(AJS.$('#gh_messages').size() > 0){
        forceSync(url, projectKey);
    }

    // Displays proper styling on JIRA Studio for Left hand navigation selection
    if(document.location.href.indexOf('GitHubConfigureRepositories') > -1){
        openJIRAStudioGitHubNavigation('menu_item_github_bulk_repo');
    }else if(document.location.href.indexOf('ConfigureGlobalSettings') > -1){
        openJIRAStudioGitHubNavigation('menu_item_github_global');
    }

}

function confirmation(delete_url) {
    var answer = confirm("Are you sure you want to remove this repository?")
    if (answer){
        window.location = delete_url;
    }
}

function toggleMoreFiles(target_div){
        AJS.$('#' + target_div).toggle();
        AJS.$('#see_more_' + target_div).toggle();
        AJS.$('#hide_more_' + target_div).toggle();
}


