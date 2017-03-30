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

package org.keycloak.testsuite.model;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.ClientLoginSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.LoginSessionModel;
import org.keycloak.testsuite.rule.KeycloakRule;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LoginSessionProviderTest {

    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    private KeycloakSession session;
    private RealmModel realm;

    @Before
    public void before() {
        session = kc.startSession();
        realm = session.realms().getRealm("test");
        session.users().addUser(realm, "user1").setEmail("user1@localhost");
        session.users().addUser(realm, "user2").setEmail("user2@localhost");
    }

    @After
    public void after() {
        resetSession();
        session.sessions().removeUserSessions(realm);
        UserModel user1 = session.users().getUserByUsername("user1", realm);
        UserModel user2 = session.users().getUserByUsername("user2", realm);

        UserManager um = new UserManager(session);
        if (user1 != null) {
            um.removeUser(realm, user1);
        }
        if (user2 != null) {
            um.removeUser(realm, user2);
        }
        kc.stopSession(session, true);
    }

    private void resetSession() {
        kc.stopSession(session, true);
        session = kc.startSession();
        realm = session.realms().getRealm("test");
    }

    @Test
    public void testLoginSessionsCRUD() {
        ClientModel client1 = realm.getClientByClientId("test-app");
        UserModel user1 = session.users().getUserByUsername("user1", realm);

        LoginSessionModel loginSession = session.loginSessions().createLoginSession(realm,client1, false);

        loginSession.setAction("foo");
        loginSession.setTimestamp(100);

        resetSession();

        // Ensure session is here
        loginSession = session.loginSessions().getLoginSession(realm, loginSession.getId());
        testLoginSession(loginSession, client1.getId(), null, "foo", 100);

        // Update and commit
        loginSession.setAction("foo-updated");
        loginSession.setTimestamp(200);
        loginSession.setAuthenticatedUser(session.users().getUserByUsername("user1", realm));

        resetSession();

        // Ensure session was updated
        loginSession = session.loginSessions().getLoginSession(realm, loginSession.getId());
        testLoginSession(loginSession, client1.getId(), user1.getId(), "foo-updated", 200);

        // Remove and commit
        session.loginSessions().removeLoginSession(realm, loginSession);

        resetSession();

        // Ensure session was removed
        Assert.assertNull(session.loginSessions().getLoginSession(realm, loginSession.getId()));

    }

    private void testLoginSession(LoginSessionModel loginSession, String expectedClientId, String expectedUserId, String expectedAction, int expectedTimestamp) {
        Assert.assertEquals(expectedClientId, loginSession.getClient().getId());
        if (expectedUserId == null) {
            Assert.assertNull(loginSession.getAuthenticatedUser());
        } else {
            Assert.assertEquals(expectedUserId, loginSession.getAuthenticatedUser().getId());
        }
        Assert.assertEquals(expectedAction, loginSession.getAction());
        Assert.assertEquals(expectedTimestamp, loginSession.getTimestamp());
    }
}
