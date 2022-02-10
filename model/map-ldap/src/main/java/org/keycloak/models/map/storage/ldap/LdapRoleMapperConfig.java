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

import org.keycloak.Config;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;

public class LdapRoleMapperConfig extends RoleMapperConfig {
    private final String realm;
    private final String clientId;

    public LdapRoleMapperConfig(Config.Scope config, String realm, String clientId) {
        super(new ComponentModel() {
            @Override
            public MultivaluedHashMap<String, String> getConfig() {
                return new MultivaluedHashMap<String, String>() {
                    @Override
                    public String getFirst(String key) {
                        if (clientId == null) {
                            return config.scope(realm).get(key);
                        } else {
                            String val = config.scope(realm).scope("clients").scope("client").scope(clientId).get(key);
                            if (val == null) {
                                val = config.scope(realm).scope("clients").get(key);
                                if (val != null) {
                                    val = val.replaceAll("\\{0}", clientId);
                                }
                            }
                            if (val == null) {
                                val = config.scope(realm).get(key);
                            }
                            return val;
                        }
                    }
                };
            }
        });
        this.clientId = clientId;
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    @Override
    public String getClientId() {
        return clientId;
    }
}
