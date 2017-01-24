<!--
~ Copyright 2016 Red Hat, Inc. and/or its affiliates
~ and other contributors as indicated by the @author tags.
~
~ Licensed under the Apache License, Version 2.0 (the "License");
~ you may not use this file except in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing, software
~ distributed under the License is distributed on an "AS IS" BASIS,
~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~ See the License for the specific language governing permissions and
~ limitations under the License.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:a="http://jboss.org/schema/arquillian"
                version="2.0"
                exclude-result-prefixes="xalan a">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="/a:arquillian">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            
            <container qualifier="app-server-${{app.server}}" mode="${{app.server.mode}}" >
                <configuration>
                    <property name="enabled">true</property>
                    <property name="adapterImplClass">org.jboss.as.arquillian.container.managed.ManagedDeployableContainer</property>
                    <property name="jbossHome">${app.server.home}</property>
                    <property name="javaHome">${app.server.java.home}</property>
                    <property name="jbossArguments">
                        -Djboss.socket.binding.port-offset=${app.server.port.offset} 
                        ${adapter.test.props}
                        -Djboss.shutdown.forceHalt=false
                    </property>
                    <property name="javaVmArguments">
                        ${app.server.memory.settings}
                        -Djava.net.preferIPv4Stack=true
                        -javaagent:/home/hmlnarik/.m2/repository/org/jacoco/org.jacoco.agent/0.7.8/org.jacoco.agent-0.7.8-runtime.jar=destfile=/home/hmlnarik/src/keycloak/target/coverage-reports/jacoco-app-server.exec
                    </property>
                    <property name="managementProtocol">${app.server.management.protocol}</property>
                    <property name="managementPort">${app.server.management.port}</property>
                    <property name="startupTimeoutInSeconds">${app.server.startup.timeout}</property>
                </configuration>
            </container>
            
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>