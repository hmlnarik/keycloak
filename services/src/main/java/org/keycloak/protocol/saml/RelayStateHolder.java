/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.saml;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.AuthenticatedClientSessionModelReadOnly;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionModelReadOnly;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author hmlnarik
 */
public class RelayStateHolder<US extends UserSessionModelReadOnly, ACS extends AuthenticatedClientSessionModelReadOnly> {

    private static final Pattern RELAY_STATE_SINGLE_CLIENT_ONLY = Pattern.compile("([0-9a-f-]+)\\.([0-9a-f-]+)", Pattern.CASE_INSENSITIVE); // <User-session-ID>.<Client-UUID>

    private final US userSession;

    private final ACS clientSession;

    public static RelayStateHolder<UserSessionModel, AuthenticatedClientSessionModel> from(KeycloakSession session, RealmModel realm, String relayStateParameter) {
        Matcher m;
        final String userSessionId;
        final String clientUuid;

        if (relayStateParameter == null) {
            return new RelayStateHolder(null, null);
        }

        m = RELAY_STATE_SINGLE_CLIENT_ONLY.matcher(relayStateParameter);
        if (m.matches()) {
            userSessionId = m.group(1);
            clientUuid = m.group(2);

            final UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
            return new RelayStateHolder(userSession, userSession == null ? null : userSession.getAuthenticatedClientSessions().get(clientUuid));
        } else {
            userSessionId = relayStateParameter;

            return new RelayStateHolder(session.sessions().getUserSession(realm, userSessionId), null);
        }
    }

    public static <ACS extends AuthenticatedClientSessionModelReadOnly> RelayStateHolder<UserSessionModelReadOnly, ACS> from(ACS clientSession) {
        if (clientSession == null) {
            return new RelayStateHolder(null, null);
        }
        return new RelayStateHolder<>(clientSession.getUserSession(), clientSession);
    }


    private RelayStateHolder(US userSession, ACS clientSession) {
        this.userSession = userSession;
        this.clientSession = clientSession;
    }

    public boolean hasClientSession() {
        return clientSession != null;
    }

    public boolean hasUserSession() {
        return userSession != null;
    }

    public US getUserSession() {
        return userSession;
    }

    public ACS getClientSession() {
        return clientSession;
    }

    public String toRelayStateParameter() {
        if (! hasUserSession()) {
            return null;
        }
        if (! hasClientSession()) {
            return userSession.getId();
        }

        return userSession.getId() + "." + clientSession.getClient().getId();
    }

    @Override
    public String toString() {
        return "RelayStateHolder{" + "userSession=" + userSession.getId() + ", clientSession=" + clientSession.getClient().getClientId() + '}';
    }
}
