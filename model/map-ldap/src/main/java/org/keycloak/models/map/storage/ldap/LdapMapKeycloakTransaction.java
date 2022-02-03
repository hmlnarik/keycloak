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

import java.util.stream.Stream;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.storage.ldap.idm.query.Condition;

// todo: might extend LDAPTransaction in the future
public abstract class LdapMapKeycloakTransaction<RE, E extends AbstractEntity & UpdatableEntity, M> implements MapKeycloakTransaction<E, M> {

    private final MapKeycloakTransaction<E, M> delegate;

    public LdapMapKeycloakTransaction(MapKeycloakTransaction<E, M> delegate) {
        this.delegate = delegate;
    }

    @Override
    public E create(E mapEntity) {
        return delegate.create(mapEntity);
    }

    @Override
    public E read(String key) {
        return delegate.read(key);
    }

    protected abstract LdapModelCriteriaBuilder createLdapModelCriteriaBuilder();

    @Override
    public Stream<E> read(QueryParameters<M> queryParameters) {
        LdapModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createLdapModelCriteriaBuilder());

        Condition condition = (Condition) mcb.getPredicateFunc().get();
        StringBuilder sb = new StringBuilder();
        condition.applyCondition(sb);

        /** TODO: pass condition to LDAP and run query **/

        /*
        LDAPQuery ldapQuery = new LDAPQuery(ldapProvider);

        // For now, use same search scope, which is configured "globally" and used for user's search.
        ldapQuery.setSearchScope(ldapProvider.getLdapIdentityStore().getConfig().getSearchScope());

        String rolesDn = config.getRolesDn();
        ldapQuery.setSearchDn(rolesDn);

        Collection<String> roleObjectClasses = config.getRoleObjectClasses(ldapProvider);
        ldapQuery.addObjectClasses(roleObjectClasses);

        String rolesRdnAttr = config.getRoleNameLdapAttribute();

        String customFilter = config.getCustomLdapFilter();
        if (customFilter != null && customFilter.trim().length() > 0) {
            Condition customFilterCondition = new LDAPQueryConditionsBuilder().addCustomLDAPFilter(customFilter);
            ldapQuery.addWhereCondition(customFilterCondition);
        }

        ldapQuery.addReturningLdapAttribute(rolesRdnAttr);

        */

        return delegate.read(queryParameters);
    }

    @Override
    public long getCount(QueryParameters<M> queryParameters) {
        return delegate.getCount(queryParameters);
    }

    @Override
    public boolean delete(String key) {
        return delegate.delete(key);
    }

    @Override
    public long delete(QueryParameters<M> queryParameters) {
        return delegate.delete(queryParameters);
    }
}
