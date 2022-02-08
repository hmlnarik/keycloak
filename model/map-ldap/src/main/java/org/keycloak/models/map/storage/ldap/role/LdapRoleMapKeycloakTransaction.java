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

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.role.MapRoleEntity;

import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.ldap.LdapConfig;
import org.keycloak.models.map.storage.ldap.LdapMapKeycloakTransaction;
import org.keycloak.models.map.storage.ldap.LdapRoleMapperConfig;
import org.keycloak.models.map.storage.ldap.role.entity.LdapRoleEntity;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.EscapeStrategy;
import org.keycloak.storage.ldap.idm.query.internal.CustomLDAPFilter;
import org.keycloak.storage.ldap.idm.query.internal.EqualCondition;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.query.internal.NoopCondition;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Stream;

public class LdapRoleMapKeycloakTransaction extends LdapMapKeycloakTransaction<LdapRoleEntity, MapRoleEntity, RoleModel> {

    private final MapKeycloakTransaction<MapRoleEntity, RoleModel> delegate;
    private final KeycloakSession session;
    private final Config.Scope config;

    public LdapRoleMapKeycloakTransaction(KeycloakSession session, Config.Scope config, MapKeycloakTransaction<MapRoleEntity, RoleModel> delegate) {
        super(session, config, delegate);
        this.session = session;
        this.config = config;
        this.delegate = delegate;
    }

    public interface LdapRoleMapKeycloakTransactionFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @Override
    public MapRoleEntity read(String key) {
        MapRoleEntity val = delegate.read(key);
        if (val == null) {
            StringTokenizer st = new StringTokenizer(key, ".");
            if (!st.hasMoreTokens()) {
                return null;
            }
            String realm = st.nextToken();
            if (!st.hasMoreTokens()) {
                return null;
            }
            String id = st.nextToken();

            LdapConfig ldapConfig = new LdapConfig(config, realm);

            // try to look it up as a realm role
            val = lookupEntityById(realm, id, ldapConfig, null);

            if (val == null) {
                // try to look it up using a client role
                // currently the API doesn't allow to get a list of all keys, therefore we need a separate attribute
                // also, getArray is broken as it doesn't look up the parent's values if an entry is empty
                String[] clientIds = config.scope(realm).scope("clients").get("clientsToSearch").split("\\s*,\\s*");
                for (String clientId : clientIds) {
                    val = lookupEntityById(realm, id, ldapConfig, clientId);
                    if (val != null) {
                        break;
                    }
                }
            }

        }
        return val;
    }

    private MapRoleEntity lookupEntityById(String realm, String id, LdapConfig ldapConfig, String clientId) {
        LdapRoleMapperConfig roleMapperConfig = new LdapRoleMapperConfig(config, realm, clientId);

        LDAPIdentityStore identityStore = new LDAPIdentityStore(session, ldapConfig);

        LDAPQuery ldapQuery = getLdapQuery(ldapConfig, roleMapperConfig);

        ldapQuery.addWhereCondition(new EqualCondition(ldapConfig.getUuidLDAPAttributeName(), id, EscapeStrategy.DEFAULT));

        List<LDAPObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);
        if (ldapObjects.size() == 1) {
            return new LdapRoleEntity(ldapObjects.get(0), roleMapperConfig);
        }
        return null;
    }

    @Override
    public Stream<MapRoleEntity> read(QueryParameters<RoleModel> queryParameters) {

        // first analyze the query to find out the realm
        LdapRoleModelCriteriaBuilderForRealm mcbr = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createLdapModelCriteriaBuilderForRealm());
        String realm;
        Optional<String> realmOptional = mcbr.getPredicateFunc().get().findAny();
        if (!realmOptional.isPresent()) {
            throw new IllegalArgumentException("unable to determine realm from query parameters");
        }
        realm = realmOptional.get();

        // find out if this contains a client ID
        LdapRoleModelCriteriaBuilderForClientId mcbc = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createLdapModelCriteriaBuilderForClientId());
        String clientId = null;
        Optional<String> clientIdOptional = mcbc.getPredicateFunc().get().findAny();
        if (clientIdOptional.isPresent()) {
          clientId = clientIdOptional.get();
        }

        // then analyze the query again to retrieve the query without the realm
        LdapRoleModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createLdapModelCriteriaBuilder());

        LdapConfig ldapConfig = new LdapConfig(config, realm);
        LdapRoleMapperConfig roleMapperConfig = new LdapRoleMapperConfig(config, realm, clientId);

        LDAPIdentityStore identityStore = new LDAPIdentityStore(session, ldapConfig);

        LDAPQuery ldapQuery = getLdapQuery(ldapConfig, roleMapperConfig);

        Condition condition = mcb.getPredicateFunc().apply(roleMapperConfig);
        if (!(condition instanceof NoopCondition)) {
            ldapQuery.addWhereCondition(condition);
        }

        List<LDAPObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);

        Stream<LdapRoleEntity> ldapStream = ldapObjects.stream().map(ldapObject -> new LdapRoleEntity(ldapObject, roleMapperConfig));

        return Stream.concat(delegate.read(queryParameters), ldapStream);
    }

    private LDAPQuery getLdapQuery(LdapConfig ldapConfig, LdapRoleMapperConfig roleMapperConfig) {
        LDAPQuery ldapQuery = new LDAPQuery(null);

        // For now, use same search scope, which is configured "globally" and used for user's search.
        ldapQuery.setSearchScope(ldapConfig.getSearchScope());

        String rolesDn = roleMapperConfig.getRolesDn();
        ldapQuery.setSearchDn(rolesDn);

        Collection<String> roleObjectClasses = ldapConfig.getRoleObjectClasses();
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

    @Override
    protected LdapRoleModelCriteriaBuilderForRealm createLdapModelCriteriaBuilderForRealm() {
        return new LdapRoleModelCriteriaBuilderForRealm();
    }

    @Override
    protected LdapRoleModelCriteriaBuilderForClientId createLdapModelCriteriaBuilderForClientId() {
        return new LdapRoleModelCriteriaBuilderForClientId();
    }

}
