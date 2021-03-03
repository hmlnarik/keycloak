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

package org.keycloak.models.jpa;

import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RealmProviderFactory;

import javax.persistence.EntityManager;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaRealmProviderFactory implements RealmProviderFactory {

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(new ProviderEventListener() {
            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof RoleContainerModel.RoleRemovedEvent) {
                    RoleContainerModel.RoleRemovedEvent roleRemovedEvent = (RoleContainerModel.RoleRemovedEvent) event;
                    JpaRealmProvider provider = (JpaRealmProvider) roleRemovedEvent.getKeycloakSession().getProvider(RealmProvider.class, getId());
                    RoleContainerModel container = roleRemovedEvent.getRole().getContainer();
                    RealmModel realm;
                    if (container instanceof RealmModel) {
                        realm = ((RealmModel) container);
                    } else if (container instanceof ClientModel) {
                        realm = ((ClientModel) container).getRealm();
                    } else {
                        return;
                    }
                    provider.preRemove(realm, roleRemovedEvent.getRole());
                }
            }
        });
    }

    @Override
    public String getId() {
        return "jpa";
    }

    @Override
    public RealmProvider create(KeycloakSession session) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        return new JpaRealmProvider(session, em);
    }

    @Override
    public void close() {
    }

}
