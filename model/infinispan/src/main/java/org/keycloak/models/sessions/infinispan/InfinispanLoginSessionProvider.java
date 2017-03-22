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

package org.keycloak.models.sessions.infinispan;

import java.util.Iterator;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.entities.LoginSessionEntity;
import org.keycloak.models.sessions.infinispan.stream.LoginSessionPredicate;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RealmInfoUtil;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.sessions.LoginSessionModel;
import org.keycloak.sessions.LoginSessionProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanLoginSessionProvider implements LoginSessionProvider {

    private static final Logger log = Logger.getLogger(InfinispanLoginSessionProvider.class);

    private final KeycloakSession session;
    private final Cache<String, LoginSessionEntity> cache;
    protected final InfinispanKeycloakTransaction tx;

    public static final String LOGIN_SESSION_ID = "LOGIN_SESSION_ID";

    public InfinispanLoginSessionProvider(KeycloakSession session, Cache<String, LoginSessionEntity> cache) {
        this.session = session;
        this.cache = cache;

        this.tx = new InfinispanKeycloakTransaction();
        session.getTransactionManager().enlistAfterCompletion(tx);
    }


    @Override
    public LoginSessionModel createLoginSession(RealmModel realm, ClientModel client, boolean browser) {
        String id = KeycloakModelUtils.generateId();

        LoginSessionEntity entity = new LoginSessionEntity();
        entity.setId(id);
        entity.setRealm(realm.getId());
        entity.setTimestamp(Time.currentTime());
        entity.setClientUuid(client.getId());

        tx.put(cache, id, entity);

        if (browser) {
            setBrowserCookie(id, realm);
        }

        LoginSessionAdapter wrap = wrap(realm, entity);
        return wrap;
    }

    private LoginSessionAdapter wrap(RealmModel realm, LoginSessionEntity entity) {
        return entity==null ? null : new LoginSessionAdapter(session, this, cache, realm, entity);
    }

    @Override
    public String getCurrentLoginSessionId(RealmModel realm) {
        return getIdFromBrowserCookie();
    }

    @Override
    public LoginSessionModel getCurrentLoginSession(RealmModel realm) {
        String loginSessionId = getIdFromBrowserCookie();
        return loginSessionId==null ? null : getLoginSession(realm, loginSessionId);
    }

    @Override
    public LoginSessionModel getLoginSession(RealmModel realm, String loginSessionId) {
        LoginSessionEntity entity = getLoginSessionEntity(realm, loginSessionId);
        return wrap(realm, entity);
    }

    private LoginSessionEntity getLoginSessionEntity(RealmModel realm, String loginSessionId) {
        LoginSessionEntity entity = cache.get(loginSessionId);

        // Chance created in this transaction TODO: should it be opposite and rather look locally first? Check performance...
        if (entity == null) {
            entity = tx.get(cache, loginSessionId);
        }

        return entity;
    }

    @Override
    public void removeLoginSession(RealmModel realm, LoginSessionModel loginSession) {
        tx.remove(cache, loginSession.getId());
    }

    @Override
    public void removeExpired(RealmModel realm) {
        log.debugf("Removing expired sessions");

        int expired = Time.currentTime() - RealmInfoUtil.getDettachedClientSessionLifespan(realm);


        // Each cluster node cleanups just local sessions, which are those owned by himself (+ few more taking l1 cache into account)
        Iterator<Map.Entry<String, LoginSessionEntity>> itr = cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL)
                .entrySet().stream().filter(LoginSessionPredicate.create(realm.getId()).expired(expired)).iterator();

        int counter = 0;
        while (itr.hasNext()) {
            counter++;
            LoginSessionEntity entity = itr.next().getValue();
            tx.remove(cache, entity.getId());
        }

        log.debugf("Removed %d expired user sessions for realm '%s'", counter, realm.getName());
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        Iterator<Map.Entry<String, LoginSessionEntity>> itr = cache.entrySet().stream().filter(LoginSessionPredicate.create(realm.getId())).iterator();
        while (itr.hasNext()) {
            cache.remove(itr.next().getKey());
        }
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        Iterator<Map.Entry<String, LoginSessionEntity>> itr = cache.entrySet().stream().filter(LoginSessionPredicate.create(realm.getId()).client(client.getId())).iterator();
        while (itr.hasNext()) {
            cache.remove(itr.next().getKey());
        }
    }

    @Override
    public void close() {

    }

    // COOKIE STUFF

    protected void setBrowserCookie(String loginSessionId, RealmModel realm) {
        String cookiePath = CookieHelper.getRealmCookiePath(realm);
        boolean sslRequired = realm.getSslRequired().isRequired(session.getContext().getConnection());
        CookieHelper.addCookie(LOGIN_SESSION_ID, loginSessionId, cookiePath, null, null, -1, sslRequired, true);

        // TODO trace with isTraceEnabled
        log.infof("Set LOGIN_SESSION_ID cookie with value %s", loginSessionId);
    }

    protected String getIdFromBrowserCookie() {
        String cookieVal = CookieHelper.getCookieValue(LOGIN_SESSION_ID);

        if (log.isTraceEnabled()) {
            if (cookieVal != null) {
                log.tracef("Found LOGIN_SESSION_ID cookie with value %s", cookieVal);
            } else {
                log.tracef("Not found LOGIN_SESSION_ID cookie");
            }
        }

        return cookieVal;
    }
}
