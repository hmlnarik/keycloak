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
import org.keycloak.models.ModelException;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.common.DeepCloner;
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

import javax.naming.NamingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
    public MapRoleEntity create(MapRoleEntity value) {
        if (Arrays.asList("admin", "default-roles-master").contains(value.getName()) || value.isClientRole()) {
            // this is a composite role, we don't support that yet
            return delegate.create(value);
        }
        // in order to get the ID, we need to write it to LDAP
        // TODO: on transaction rollback the element should then be deleted

        LdapRoleMapperConfig roleMapperConfig = new LdapRoleMapperConfig(config, value.getRealmId(), value.getClientId());
        LdapConfig ldapConfig = new LdapConfig(config, value.getRealmId());
        LDAPIdentityStore identityStore = new LDAPIdentityStore(session, ldapConfig);

        DeepCloner CLONER = new DeepCloner.Builder()
                .constructor(MapRoleEntity.class, cloner -> new LdapRoleEntity(cloner, roleMapperConfig))
                .build();

        LdapRoleEntity mapped = (LdapRoleEntity) CLONER.from(value);
        // LDAP should never use the UUID provided by the caller, as UUID is generated
        mapped.setId(null);
        // Roles as groups need to have at least one member on most directories. Add ourselves as a member as a dummy.
        if (mapped.getLdapObject().getUuid() == null && mapped.getLdapObject().getAttributeAsSet("member") == null) {
            // insert our own name as dummy member of this role to avoid a schema conflict in LDAP
            mapped.getLdapObject().setAttribute("member", Stream.of(mapped.getLdapObject().getDn().toString()).collect(Collectors.toSet()));
        }

        identityStore.add(mapped.getLdapObject());

        entities.put(new EntityKey(mapped.getRealmId(), mapped.getId()), mapped);

        tasksOnRollback.add(new DeleteOperation(mapped) {
            @Override
            public void execute() {
                identityStore.remove(mapped.getLdapObject());
                entities.remove(new EntityKey(mapped.getRealmId(), mapped.getId()));
            }
        });

        return mapped;
    }

    @Override
    public boolean delete(String key) {
        boolean delete = super.delete(key);
        MapRoleEntity read = read(key);
        if (read instanceof LdapRoleEntity) {
            tasksOnCommit.add(new DeleteOperation((LdapRoleEntity) read) {
                @Override
                public void execute() {
                    LdapConfig ldapConfig = new LdapConfig(config, entity.getRealmId());
                    LDAPIdentityStore identityStore = new LDAPIdentityStore(session, ldapConfig);
                    identityStore.remove(entity.getLdapObject());
                }
            });
            delete = true;
        }
        return delete;
    }

    @Override
    public MapRoleEntity read(String key) {
        MapRoleEntity val = delegate.read(key);
        if (val == null) {
            // for now, only support one realm, don't make realm part of the key
            // https://github.com/keycloak/keycloak/discussions/10045
            /*
            StringTokenizer st = new StringTokenizer(key, ".");
            if (!st.hasMoreTokens()) {
                return null;
            }
            String realm = st.nextToken();
            if (!st.hasMoreTokens()) {
                return null;
            }
            String id = st.nextToken();
             */
            String realm = "master";
            @SuppressWarnings("UnnecessaryLocalVariable") String id = key;

            // reuse an existing live entity
            val = entities.get(new EntityKey(realm, id));

            if (val == null) {
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

                if (val != null) {
                    entities.put(new EntityKey(val.getRealmId(), val.getId()), (LdapRoleEntity) val);
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

        Stream<LdapRoleEntity> ldapStream;

        try {
            List<LDAPObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);

            ldapStream = ldapObjects.stream().map(ldapObject -> {
                LdapRoleEntity ldapRoleEntity = new LdapRoleEntity(ldapObject, roleMapperConfig);
                LdapRoleEntity existingEntry = entities.get(new EntityKey(ldapRoleEntity.getRealmId(), ldapRoleEntity.getId()));
                if (existingEntry != null) {
                    // TODO: instead of returning the existing entity,
                    //  filter out this entity, and search all existing entries using the query criteria and append those result.
                    return existingEntry;
                }
                // TODO: this assumes that these are no light entities, that would later need loading of details
                entities.put(new EntityKey(ldapRoleEntity.getRealmId(), ldapRoleEntity.getId()), ldapRoleEntity);
                return ldapRoleEntity;
            });
        } catch (ModelException ex) {
            if (clientId != null && ex.getCause() instanceof NamingException) {
                // the client wasn't found in LDAP, assume an empty result
                ldapStream = Stream.empty();
            } else {
                throw ex;
            }
        }

        return Stream.concat(delegate.read(queryParameters), ldapStream).sorted(Comparator.comparing(MapRoleEntity::getName));
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
        ldapQuery.addReturningLdapAttribute("description");
        return ldapQuery;
    }

    @Override
    public void begin() {
        delegate.begin();
    }

    @Override
    public void commit() {
        delegate.commit();

        for (MapTaskWithValue mapTaskWithValue : tasksOnCommit) {
            mapTaskWithValue.execute();
        }

        entities.forEach((entityKey, ldapRoleEntity) -> {
            if (ldapRoleEntity.isUpdated()) {
                LdapConfig ldapConfig = new LdapConfig(config, entityKey.getRealmId());
                LDAPIdentityStore identityStore = new LDAPIdentityStore(session, ldapConfig);
                identityStore.update(ldapRoleEntity.getLdapObject());
            }
        });
    }

    @Override
    public void rollback() {
        delegate.rollback();

        for (MapTaskWithValue mapTaskWithValue : tasksOnRollback) {
            mapTaskWithValue.execute();
        }
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
