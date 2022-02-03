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

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class LdapConfig extends LDAPConfig {
    private MultivaluedHashMap<String, String> config;

    public LdapConfig(MultivaluedHashMap<String, String> config) {
        super(config);
        this.config = config;
    }

    // from: RoleMapperConfig
    public Collection<String> getRoleObjectClasses() {
        String objectClasses = config.getFirst(RoleMapperConfig.ROLE_OBJECT_CLASSES);
        if (objectClasses == null) {
            // For Active directory, the default is 'group' . For other servers 'groupOfNames'
            objectClasses = isActiveDirectory() ? LDAPConstants.GROUP : LDAPConstants.GROUP_OF_NAMES;
        }

        return getConfigValues(objectClasses);
    }

    // from: RoleMapperConfig
    protected Set<String> getConfigValues(String str) {
        String[] objClasses = str.split(",");
        Set<String> trimmed = new HashSet<>();
        for (String objectClass : objClasses) {
            objectClass = objectClass.trim();
            if (objectClass.length() > 0) {
                trimmed.add(objectClass);
            }
        }
        return trimmed;
    }

}
