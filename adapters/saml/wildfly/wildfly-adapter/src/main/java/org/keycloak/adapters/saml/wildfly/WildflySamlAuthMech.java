/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.saml.wildfly;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;

import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.undertow.ServletSamlAuthMech;
import org.keycloak.adapters.spi.*;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;

import io.undertow.servlet.api.DeploymentInfo;
import java.lang.reflect.*;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WildflySamlAuthMech extends ServletSamlAuthMech {

    private static final Logger LOG = Logger.getLogger(WildflySamlAuthMech.class);

    public WildflySamlAuthMech(SamlDeploymentContext deploymentContext, UndertowUserSessionManagement sessionManagement, String errorPage) {
        super(deploymentContext, sessionManagement, errorPage);
    }

    @Override
    public void addTokenStoreUpdaters(DeploymentInfo deploymentInfo) {
        super.addTokenStoreUpdaters(deploymentInfo);

        Map<String, String> initParameters = deploymentInfo.getInitParameters();
        String idMapperSessionUpdaterClasses = initParameters == null
          ? null
          : initParameters.get("keycloak.sessionIdMapperUpdater.classes");
        if (idMapperSessionUpdaterClasses == null) {
            return;
        }

        for (String clazz : idMapperSessionUpdaterClasses.split("\\s*,\\s*")) {
            if (! clazz.isEmpty()) {
                invokeAddTokenStoreUpdaterMethod(clazz, deploymentInfo);
            }
        }
    }

    private void invokeAddTokenStoreUpdaterMethod(String idMapperSessionUpdaterClass, DeploymentInfo deploymentInfo) {
        try {
            Class<?> clazz = Class.forName(idMapperSessionUpdaterClass);
            Method addTokenStoreUpdatersMethod = clazz.getMethod("addTokenStoreUpdaters", DeploymentInfo.class, SessionIdMapper.class);
            if (! Modifier.isStatic(addTokenStoreUpdatersMethod.getModifiers())) {
                LOG.warnv("addTokenStoreUpdaters method in class {0} has to be static.", idMapperSessionUpdaterClass);
                return;
            }

            if (! addTokenStoreUpdatersMethod.isAccessible()) {
                addTokenStoreUpdatersMethod.setAccessible(true);    // TODO: Elevate privileges for security manager
            }

            LOG.debugv("Initializing sessionIdMapperUpdater class {0}", idMapperSessionUpdaterClass);
            addTokenStoreUpdatersMethod.invoke(null, deploymentInfo, idMapper);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException ex) {
            LOG.warnv(ex, "Cannot use sessionIdMapperUpdater class {0}", idMapperSessionUpdaterClass);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOG.warnv(ex, "Cannot use {0}.addTokenStoreUpdaters(DeploymentInfo, SessionIdMapper) method", idMapperSessionUpdaterClass);
        }
    }

    @Override
    protected SamlSessionStore getTokenStore(HttpServerExchange exchange, HttpFacade facade, SamlDeployment deployment, SecurityContext securityContext) {
        return new WildflySamlSessionStore(exchange, sessionManagement, securityContext, idMapper, SessionIdMapperUpdater.EXTERNAL, deployment);
    }
}
