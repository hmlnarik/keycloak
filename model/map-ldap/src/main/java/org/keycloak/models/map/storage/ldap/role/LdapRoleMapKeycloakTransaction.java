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
package org.keycloak.models.map.storage.ldap.role;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.role.MapRoleEntity;

import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.ldap.LdapConfig;
import org.keycloak.models.map.storage.ldap.LdapMapKeycloakTransaction;
import org.keycloak.models.map.storage.ldap.LdapModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.role.entity.LdapRoleEntity;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.EscapeStrategy;
import org.keycloak.storage.ldap.idm.query.internal.CustomLDAPFilter;
import org.keycloak.storage.ldap.idm.query.internal.EqualCondition;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.query.internal.NoopCondition;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class LdapRoleMapKeycloakTransaction extends LdapMapKeycloakTransaction<LdapRoleEntity, MapRoleEntity, RoleModel> {

    private final MapKeycloakTransaction<MapRoleEntity, RoleModel> delegate;
    private final KeycloakSession session;
    private final LdapConfig config;
    private final RoleMapperConfig roleMapperConfig;

    public LdapRoleMapKeycloakTransaction(KeycloakSession session, LdapConfig config, RoleMapperConfig roleMapperConfig, MapKeycloakTransaction<MapRoleEntity, RoleModel> delegate) {
        super(session, config, roleMapperConfig, delegate);
        this.session = session;
        this.config = config;
        this.roleMapperConfig = roleMapperConfig;
        this.delegate = delegate;
    }

    public interface LdapRoleMapKeycloakTransactionFunction<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    @Override
    public MapRoleEntity read(String key) {
        MapRoleEntity val = delegate.read(key);
        if (val == null) {
            LDAPIdentityStore identityStore = new LDAPIdentityStore(session, config);

            LDAPQuery ldapQuery = getLdapQuery();

            ldapQuery.addWhereCondition(new EqualCondition(config.getUuidLDAPAttributeName(), key, EscapeStrategy.DEFAULT));

            List<LDAPObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);
            if (ldapObjects.size() == 1) {
                val = new LdapRoleEntity(ldapObjects.get(0), roleMapperConfig);
            }

        }
        return val;
    }

    @Override
    public Stream<MapRoleEntity> read(QueryParameters<RoleModel> queryParameters) {
        LdapModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createLdapModelCriteriaBuilder());

        LDAPIdentityStore identityStore = new LDAPIdentityStore(session, config);

        LDAPQuery ldapQuery = getLdapQuery();

        List<LDAPObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);

        Condition condition = (Condition) mcb.getPredicateFunc().get();
        if (!(condition instanceof NoopCondition)) {
            ldapQuery.addWhereCondition(condition);
        }

        Stream<LdapRoleEntity> ldapStream = ldapObjects.stream().map(ldapObject -> new LdapRoleEntity(ldapObject, roleMapperConfig));

        return Stream.concat(delegate.read(queryParameters), ldapStream);
    }

    private LDAPQuery getLdapQuery() {
        LDAPQuery ldapQuery = new LDAPQuery(null);

        // For now, use same search scope, which is configured "globally" and used for user's search.
        ldapQuery.setSearchScope(config.getSearchScope());

        String rolesDn = roleMapperConfig.getRolesDn();
        ldapQuery.setSearchDn(rolesDn);

        Collection<String> roleObjectClasses = config.getRoleObjectClasses();
        ldapQuery.addObjectClasses(roleObjectClasses);

        String rolesRdnAttr = roleMapperConfig.getRoleNameLdapAttribute();

        String customFilter = roleMapperConfig.getCustomLdapFilter();
        if (customFilter != null && customFilter.trim().length() > 0) {
            Condition customFilterCondition = new CustomLDAPFilter(customFilter);
            ldapQuery.addWhereCondition(customFilterCondition);
        }

        ldapQuery.addReturningLdapAttribute(rolesRdnAttr);
        return ldapQuery;
    }

    @Override
    public void begin() {
        delegate.begin();
    }

    @Override
    public void commit() {
        delegate.commit();
    }

    @Override
    public void rollback() {
        delegate.rollback();
    }

    @Override
    public void setRollbackOnly() {
        delegate.setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() {
        return delegate.getRollbackOnly();
    }

    @Override
    public boolean isActive() {
        return delegate.isActive();
    }

    @Override
    protected LdapRoleModelCriteriaBuilder createLdapModelCriteriaBuilder() {
        return new LdapRoleModelCriteriaBuilder();
    }
}
