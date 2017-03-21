<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${msg("pageExpiredPage")}
    <#elseif section = "header">
        ${msg("pageExpiredPage")}
    <#elseif section = "form">
        <p id="instruction1" class="instruction">
            Page is expired. To restart login <a href="${url.loginRestartFlowUrl}">${msg("doClickHere")}</a> . To continue with latest step <a href="${url.loginAction}">${msg("doClickHere")}</a>.
        </p>
    </#if>
</@layout.registrationLayout>