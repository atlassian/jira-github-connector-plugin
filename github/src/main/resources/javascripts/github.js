


window.onload = function(){
    if(AJS.$('#gh_messages').size() > 0){
        forceSync(url, projectKey);
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

AJS.$(document).ready(function(){






});

