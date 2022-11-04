/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.transaction;

import org.keycloak.common.util.Resteasy;
import static org.keycloak.services.resources.KeycloakApplication.getSessionFactory;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import io.vertx.ext.web.RoutingContext;
import org.jboss.resteasy.core.ResteasyContext;

/**
 * <p>A {@link TransactionalSessionHandler} is responsible for managing transaction sessions and its lifecycle. Its subtypes
 * are usually related to components available from the underlying stack that runs on top of the request processing chain
 * as well as at the end in order to create transaction sessions and close them accordingly, respectively.
 */
public interface TransactionalSessionHandler {

    /**
     * Creates a transactional {@link KeycloakSession}.
     *
     * @return a transactional keycloak session
     */
    default KeycloakSession create() {
        KeycloakSessionFactory sessionFactory = getSessionFactory();
        KeycloakSession session = sessionFactory.create();
        session.getTransactionManager().begin();
        return session;
    }

    default void markSessionUsed() {
    }

    /**
     * Closes a transactional {@link KeycloakSession}.
     *
     * @param session a transactional session
     */
    default void close() {
        RoutingContext context = ResteasyContext.getContextData(RoutingContext.class);

        if (context != null) {
            close(context);
        }
    }

    default void close(RoutingContext context) {
        // Do not use Resteasy.getContextData(KeycloakSession.class) as this would
        // fall back to context lookup anyway, see ResteasyVertxProvider.getContextData
        KeycloakSession session = (KeycloakSession) context.data().replace(KeycloakSession.class.getName(), null);
        if (session != null) {
            Resteasy.pushContext(KeycloakSession.class, null);  // Clear both Resteasy and Vert.X contexts
            session.close();
        }
    }
}
