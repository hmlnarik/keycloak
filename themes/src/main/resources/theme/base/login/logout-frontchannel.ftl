<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${msg("logoutTitle",(realm.displayName!''))}
    <#elseif section = "header">
        ${msg("logoutTitleHtml",(realm.displayNameHtml!''))}
    <#elseif section = "form">

        <#list clientLogoutGeneratorUri as uri>
            <iframe src="${uri}"></iframe>
        </#list>

<script>
//
//var n = 10;
//function autoSubmitCountdown(){
//    var c=n;
//    setInterval(function(){
//        if(c>=0){
//    	     document.getElementById("counter").textContent = "The form will be submitted in " + c + " seconds";
//        }
//        if(c==0){
//	    document.forms[0].submit();
//        }
//        c--;
//    },1000);
//}
//
//// Start
//autoSubmitCountdown();
//
</script>
    </#if>

</@layout.registrationLayout>
