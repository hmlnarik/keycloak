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

package org.keycloak.services.managers;

import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionManager {

    private static final String AUTH_SESSION_ID = "AUTH_SESSION_ID";

    private static final Logger log = Logger.getLogger(AuthenticationSessionManager.class);

    private final KeycloakSession session;

    public AuthenticationSessionManager(KeycloakSession session) {
        this.session = session;
    }


    public AuthenticationSessionModel createAuthenticationSession(RealmModel realm, ClientModel client, boolean browserCookie) {
        AuthenticationSessionModel authSession = session.authenticationSessions().createAuthenticationSession(realm, client);

        if (browserCookie) {
            setAuthSessionCookie(authSession.getId(), realm);
        }

        return authSession;
    }


    public String getCurrentAuthenticationSessionId(RealmModel realm) {
        return getAuthSessionCookie();
    }


    public AuthenticationSessionModel getCurrentAuthenticationSession(RealmModel realm) {
        String authSessionId = getAuthSessionCookie();
        return authSessionId==null ? null : session.authenticationSessions().getAuthenticationSession(realm, authSessionId);
    }


    public void setAuthSessionCookie(String authSessionId, RealmModel realm) {
        UriInfo uriInfo = session.getContext().getUri();
        String cookiePath = AuthenticationManager.getRealmCookiePath(realm, uriInfo);

        boolean sslRequired = realm.getSslRequired().isRequired(session.getContext().getConnection());
        CookieHelper.addCookie(AUTH_SESSION_ID, authSessionId, cookiePath, null, null, -1, sslRequired, true);

        // TODO trace with isTraceEnabled
        log.infof("Set AUTH_SESSION_ID cookie with value %s", authSessionId);
    }


    public String getAuthSessionCookie() {
        String cookieVal = CookieHelper.getCookieValue(AUTH_SESSION_ID);

        if (log.isTraceEnabled()) {
            if (cookieVal != null) {
                log.tracef("Found AUTH_SESSION_ID cookie with value %s", cookieVal);
            } else {
                log.tracef("Not found AUTH_SESSION_ID cookie");
            }
        }

        return cookieVal;
    }


    public void removeAuthenticationSession(RealmModel realm, AuthenticationSessionModel authSession, boolean expireRestartCookie) {
        log.infof("Removing authSession '%s' and expire restart-cookie", authSession.getId());
        session.authenticationSessions().removeAuthenticationSession(realm, authSession);

        // expire restart cookie
        ClientConnection clientConnection = session.getContext().getConnection();
        UriInfo uriInfo = session.getContext().getUri();
        RestartLoginCookie.expireRestartCookie(realm, clientConnection, uriInfo);
    }

}
