/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.ldap;

import java.util.HashMap;
import java.util.Map;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.ldap.role.LdapRoleMapKeycloakTransaction;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;

public class LdapMapStorageProviderFactory implements
        AmphibianProviderFactory<MapStorageProvider>,
        MapStorageProviderFactory,
        EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "ldap-map-storage";

    private volatile MapStorageProvider delegate;

    private Config.Scope config;
    private static final Logger logger = Logger.getLogger(LdapMapStorageProviderFactory.class);

    @SuppressWarnings("rawtypes")
    private static final Map<Class<?>, LdapRoleMapKeycloakTransaction.LdapRoleMapKeycloakTransactionFunction<KeycloakSession, LdapConfig, RoleMapperConfig, MapKeycloakTransaction, MapKeycloakTransaction>> MODEL_TO_TX = new HashMap<>();
    static {
        MODEL_TO_TX.put(RoleModel.class,            LdapRoleMapKeycloakTransaction::new);
    }

    public <M, V extends AbstractEntity> MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session, Class<M> modelType, MapKeycloakTransaction<V, M> delegate) {
        LdapConfig ldapConfig = new LdapConfig(new MultivaluedHashMap<String, String>() {
            @Override
            public String getFirst(String key) {
                return config.get(key);
            }
        });
        RoleMapperConfig roleMapperConfig = new RoleMapperConfig(new ComponentModel() {
            @Override
            public MultivaluedHashMap<String, String> getConfig() {
                return new MultivaluedHashMap<String, String>() {
                    @Override
                    public String getFirst(String key) {
                        return config.get(key);
                    }
                };
            }
        });
        return MODEL_TO_TX.get(modelType).apply(session, ldapConfig, roleMapperConfig, delegate);
    }

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        lazyInit(session);
        return new LdapMapStorageProvider(this, session, delegate);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "LDAP Map Storage";
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }

    @Override
    public void close() {
        if (delegate != null) {
            delegate.close();
        }
    }

    private void lazyInit(KeycloakSession session) {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = session.getProvider(MapStorageProvider.class, "concurrenthashmap");

                    /* TODO: connect to LDAP, print some diagnostics information */
                }
            }
        }
    }

}
