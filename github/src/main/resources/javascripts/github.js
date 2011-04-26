

AJS.$(document).ready(function(){
    console.log("GitHub.js");

    AJS.$(".see_more").bind('click',function(index){
        var target_div = AJS.$(this).attr('target_div');

        AJS.$('#' + target_div).toggle();
        AJS.$('#see_more_' + target_div).toggle();
        AJS.$('#hide_more_' + target_div).toggle();

    });

    AJS.$(".hide_more").bind('click',function(index){
        var target_div = AJS.$(this).attr('target_div');

        AJS.$('#' + target_div).toggle();
        AJS.$('#see_more_' + target_div).toggle();
        AJS.$('#hide_more_' + target_div).toggle();

    });

    AJS.$(".commit_date").each(function(index){
        console.log(AJS.$(this).text());
        console.log(AJS.Date.getFineRelativeDate(AJS.$(this).text()));

        AJS.$(this).text(AJS.Date.getFineRelativeDate(AJS.$(this).text()));

    });


    console.log("end GitHub.js");

});
