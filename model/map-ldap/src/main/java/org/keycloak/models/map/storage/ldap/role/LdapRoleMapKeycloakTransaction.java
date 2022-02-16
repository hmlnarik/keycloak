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
import org.keycloak.models.map.common.StringKeyConvertor;
import org.keycloak.models.map.role.MapRoleEntity;

import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.LdapConfig;
import org.keycloak.models.map.storage.ldap.LdapMapKeycloakTransaction;
import org.keycloak.models.map.storage.ldap.LdapRoleMapperConfig;
import org.keycloak.models.map.storage.ldap.condition.CustomLDAPFilter;
import org.keycloak.models.map.storage.ldap.condition.EqualCondition;
import org.keycloak.models.map.storage.ldap.condition.NoopCondition;
import org.keycloak.models.map.storage.ldap.role.entity.LdapRoleEntity;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.EscapeStrategy;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;

import javax.naming.NamingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LdapRoleMapKeycloakTransaction extends LdapMapKeycloakTransaction<LdapRoleEntity, MapRoleEntity, RoleModel> {

    private final KeycloakSession session;
    private final Config.Scope config;
    private final StringKeyConvertor<String> keyConverter = new StringKeyConvertor.StringKey();
    private final List<String> deletedKeys = new LinkedList<>();

    public LdapRoleMapKeycloakTransaction(KeycloakSession session, Config.Scope config, MapKeycloakTransaction<MapRoleEntity, RoleModel> delegate) {
        super(session, config);
        this.session = session;
        this.config = config;
    }

    // interface matching the constructor of this class
    public interface LdapRoleMapKeycloakTransactionFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    // TODO: entries might get stale if a DN of an entry changes due to changes in the entity in the same transaction
    private final Map<String, String> dns = new HashMap<>();

    public String readIdByDn(String realmId, String dn) {
        // TODO: this might not be necessary if the LDAP server would support an extended OID
        // https://ldapwiki.com/wiki/LDAP_SERVER_EXTENDED_DN_OID

        String id = dns.get(dn);
        if (id == null) {
            for (Map.Entry<EntityKey, LdapRoleEntity> entry : entities.entrySet()) {
                if (entry.getValue().getLdapObject().getUuid().equals(dn)) {
                    id = entry.getValue().getLdapObject().getUuid();
                    break;
                }
            }
        }
        if (id != null) {
            return id;
        }

        LdapConfig ldapConfig = new LdapConfig(config, realmId);

        LDAPQuery ldapQuery = new LDAPQuery(null);
        LdapRoleMapperConfig roleMapperConfig = new LdapRoleMapperConfig(config, realmId, null);

        // For now, use same search scope, which is configured "globally" and used for user's search.
        ldapQuery.setSearchScope(ldapConfig.getSearchScope());

        // TODO: make dynamic
        String rolesDn = "dc=keycloak,dc=org";
        ldapQuery.setSearchDn(rolesDn);

        // TODO: read them properly to be able to store them in the transaction so they are cached?!
        Collection<String> roleObjectClasses = ldapConfig.getRoleObjectClasses();
        ldapQuery.addObjectClasses(roleObjectClasses);

        String rolesRdnAttr = roleMapperConfig.getRoleNameLdapAttribute();

        String customFilter = roleMapperConfig.getCustomLdapFilter();
        if (customFilter != null && customFilter.trim().length() > 0) {
            Condition customFilterCondition = new CustomLDAPFilter(customFilter);
            ldapQuery.addWhereCondition(customFilterCondition);
        }

        ldapQuery.addReturningLdapAttribute(rolesRdnAttr);

        LDAPIdentityStore identityStore = new LDAPIdentityStore(session, ldapConfig);

        LDAPDn.RDN rdn = LDAPDn.fromString(dn).getFirstRdn();
        String key = rdn.getAllKeys().get(0);
        String value = rdn.getAttrValue(key);
        ldapQuery.addWhereCondition(new EqualCondition(key, value, EscapeStrategy.DEFAULT));

        List<LDAPObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);
        if (ldapObjects.size() == 1) {
            dns.put(dn, ldapObjects.get(0).getUuid());
            return ldapObjects.get(0).getUuid();
        }
        return null;
    }

    private MapModelCriteriaBuilder<String, LdapRoleEntity, RoleModel> createCriteriaBuilderMap() {
        return new MapModelCriteriaBuilder<>(keyConverter, MapFieldPredicates.getPredicates(RoleModel.class));
    }

    @Override
    public MapRoleEntity create(MapRoleEntity value) {
        /*
        if (Arrays.asList("admin", "default-roles-master", "view-clients", "view-users", "manage-account", "manage-consent").contains(value.getName())) {
            // this is a composite role, we don't support that yet
            return delegate.create(value);
        }
         */
        // in order to get the ID, we need to write it to LDAP

        LdapRoleMapperConfig roleMapperConfig = new LdapRoleMapperConfig(config, value.getRealmId(), value.getClientId());
        LdapConfig ldapConfig = new LdapConfig(config, value.getRealmId());
        LDAPIdentityStore identityStore = new LDAPIdentityStore(session, ldapConfig);

        DeepCloner CLONER = new DeepCloner.Builder()
                .constructor(MapRoleEntity.class, cloner -> new LdapRoleEntity(cloner, roleMapperConfig, this))
                .build();

        LdapRoleEntity mapped = (LdapRoleEntity) CLONER.from(value);
        // LDAP should never use the UUID provided by the caller, as UUID is generated
        mapped.setId(null);
        // Roles as groups need to have at least one member on most directories. Add ourselves as a member as a dummy.
        if (mapped.getLdapObject().getUuid() == null && mapped.getLdapObject().getAttributeAsSet(roleMapperConfig.getMembershipLdapAttribute()) == null) {
            // insert our own name as dummy member of this role to avoid a schema conflict in LDAP
            mapped.getLdapObject().setAttribute(roleMapperConfig.getMembershipLdapAttribute(), Stream.of(mapped.getLdapObject().getDn().toString()).collect(Collectors.toSet()));
        }

        try {
            identityStore.add(mapped.getLdapObject());
        } catch (ModelException ex) {
            if (value.isClientRole() && ex.getCause() instanceof NamingException) {
                // the client hasn't been created, therefore adding it here
                LDAPObject client = new LDAPObject();
                client.setObjectClasses(Arrays.asList("top", "organizationalUnit"));
                client.setRdnAttributeName("ou");
                client.setDn(LDAPDn.fromString(roleMapperConfig.getRolesDn()));
                client.setSingleAttribute("ou", mapped.getClientId());
                identityStore.add(client);

                tasksOnRollback.add(new DeleteOperation(mapped) {
                    @Override
                    public void execute() {
                        identityStore.remove(client);
                    }
                });

                // retry creation of client role
                identityStore.add(mapped.getLdapObject());
            }
        }

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
        LdapRoleEntity read = read(key);
        if (read == null) {
            throw new ModelException("unable to read entity with key " + key);
        }
        deletedKeys.add(key);
        tasksOnCommit.add(new DeleteOperation(read) {
            @Override
            public void execute() {
                LdapConfig ldapConfig = new LdapConfig(config, entity.getRealmId());
                LDAPIdentityStore identityStore = new LDAPIdentityStore(session, ldapConfig);
                identityStore.remove(entity.getLdapObject());
            }
        });
        return true;
    }

    @Override
    public LdapRoleEntity read(String key) {
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

        if (deletedKeys.contains(key)) {
            return null;
        }

        // reuse an existing live entity
        LdapRoleEntity val = entities.get(new EntityKey(realm, id));

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

            if (val == null) {
                // try to find out the client ID
                LDAPQuery ldapQuery = new LDAPQuery(null);

                // For now, use same search scope, which is configured "globally" and used for user's search.
                ldapQuery.setSearchScope(ldapConfig.getSearchScope());

                // remove prefix with placeholder to allow for a broad search
                ldapQuery.setSearchDn(config.scope(realm).scope("clients").get("roles.dn").replaceAll(".*\\{0},", ""));

                ldapQuery.addWhereCondition(new EqualCondition(ldapConfig.getUuidLDAPAttributeName(), id, EscapeStrategy.DEFAULT));

                LDAPIdentityStore identityStore = new LDAPIdentityStore(session, ldapConfig);

                List<LDAPObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);
                if (ldapObjects.size() == 1) {
                    // as the client ID is now known, search again with the specific configuration
                    String clientId = ldapObjects.get(0).getDn().getParentDn().getFirstRdn().getAttrValue("ou");
                    // TODO: re-use the ldapObject to create the entity instead of querying again
                    val = lookupEntityById(realm, id, ldapConfig, clientId);
                }
            }

            if (val != null) {
                entities.put(new EntityKey(val.getRealmId(), val.getId()), val);
            }

        }
        return val;
    }

    private LdapRoleEntity lookupEntityById(String realm, String id, LdapConfig ldapConfig, String clientId) {
        LdapRoleMapperConfig roleMapperConfig = new LdapRoleMapperConfig(config, realm, clientId);

        LDAPIdentityStore identityStore = new LDAPIdentityStore(session, ldapConfig);

        LDAPQuery ldapQuery = getLdapQuery(ldapConfig, roleMapperConfig);

        ldapQuery.addWhereCondition(new EqualCondition(ldapConfig.getUuidLDAPAttributeName(), id, EscapeStrategy.DEFAULT));

        List<LDAPObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);
        if (ldapObjects.size() == 1) {
            return new LdapRoleEntity(ldapObjects.get(0), roleMapperConfig, this);
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

        Stream<MapRoleEntity> ldapStream;

        MapModelCriteriaBuilder<String,LdapRoleEntity,RoleModel> mapMcb = queryParameters.getModelCriteriaBuilder().flashToModelCriteriaBuilder(createCriteriaBuilderMap());

        Stream<LdapRoleEntity> existingEntities = entities.entrySet().stream()
                .filter(me -> mapMcb.getKeyFilter().test(keyConverter.fromString(me.getKey().getKey())) || deletedKeys.contains(me.getKey().getKey()))
                .map(Map.Entry::getValue)
                .filter(mapMcb.getEntityFilter())
                // snapshot list
                .collect(Collectors.toList()).stream();

        try {
            List<LDAPObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);

            ldapStream = ldapObjects.stream().map(ldapObject -> {
                        LdapRoleEntity ldapRoleEntity = new LdapRoleEntity(ldapObject, roleMapperConfig, this);
                        LdapRoleEntity existingEntry = entities.get(new EntityKey(ldapRoleEntity.getRealmId(), ldapRoleEntity.getId()));
                        if (existingEntry != null) {
                            // this entry will be part of the existing entities
                            return null;
                        }
                        entities.put(new EntityKey(ldapRoleEntity.getRealmId(), ldapRoleEntity.getId()), ldapRoleEntity);
                        return (MapRoleEntity) ldapRoleEntity;
                    })
                    .filter(Objects::nonNull)
                    // snapshot list
                    .collect(Collectors.toList()).stream();
        } catch (ModelException ex) {
            if (clientId != null && ex.getCause() instanceof NamingException) {
                // the client wasn't found in LDAP, assume an empty result
                ldapStream = Stream.empty();
            } else {
                throw ex;
            }
        }

        ldapStream = Stream.concat(ldapStream, existingEntities);

        if (!queryParameters.getOrderBy().isEmpty()) {
            ldapStream = ldapStream.sorted(MapFieldPredicates.getComparator(queryParameters.getOrderBy().stream()));
        }

        return ldapStream;
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
        ldapQuery.addReturningLdapAttribute(roleMapperConfig.getMembershipLdapAttribute());
        roleMapperConfig.getRoleAttributes().forEach(ldapQuery::addReturningLdapAttribute);
        return ldapQuery;
    }

    @Override
    public void commit() {
        super.commit();
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
        super.rollback();
        Iterator<MapTaskWithValue> iterator = tasksOnRollback.descendingIterator();
        while (iterator.hasNext()) {
            iterator.next().execute();
        }
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
