


window.onload = function(){
    if(AJS.$('#gh_messages')){
        forceSync(url, projectKey);
    }
}

function confirmation(delete_url) {
    var answer = confirm("Are you sure you want to remove this repository?")
    if (answer){
        window.location = delete_url;
    }
}

AJS.$(document).ready(function(){

    AJS.$(".see_more").bind("click",function(A){var B=AJS.$(this).attr("target_div");
    console.log("target_div: "+B);
    AJS.$("#"+B).toggle();
    AJS.$("#see_more_"+B).toggle();
    AJS.$("#hide_more_"+B).toggle()
    });

    AJS.$(".hide_more").bind("click",function(A){var B=AJS.$(this).attr("target_div");
    AJS.$("#"+B).toggle();
    AJS.$("#see_more_"+B).toggle();
    AJS.$("#hide_more_"+B).toggle()
    });




});

