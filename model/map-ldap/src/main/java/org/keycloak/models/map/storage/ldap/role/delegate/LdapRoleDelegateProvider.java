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
package org.keycloak.models.map.storage.ldap.role.delegate;

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.role.MapRoleEntity;
import org.keycloak.models.map.storage.ldap.LdapDelegateProvider;

public class LdapRoleDelegateProvider extends LdapDelegateProvider<MapRoleEntity> implements DelegateProvider<MapRoleEntity> {

    private final MapRoleEntity delegate;

    public LdapRoleDelegateProvider(MapRoleEntity delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public MapRoleEntity getDelegate(boolean isRead, Enum<? extends EntityField<MapRoleEntity>> field, Object... parameters) {
        return delegate;
    }

}
