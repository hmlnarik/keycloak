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

import org.keycloak.models.RoleModel;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.role.LdapRoleModelCriteriaBuilder;
import org.keycloak.storage.SearchableModelField;
import org.keycloak.storage.ldap.idm.query.EscapeStrategy;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Abstract class containing methods common to all Ldap*ModelCriteriaBuilder implementations
 * 
 * @param <E> Entity
 * @param <M> Model
 * @param <Self> specific implementation of this class
 */
public class LdapModelCriteriaBuilderForRealm<E, M, Self extends LdapModelCriteriaBuilderForRealm<E, M, Self>> implements ModelCriteriaBuilder<M, Self> {

    private final Function<Supplier<Stream<String>>, Self> instantiator;
    private Supplier<Stream<String>> predicateFunc = null;

    public LdapModelCriteriaBuilderForRealm(Function<Supplier<Stream<String>>, Self> instantiator) {
        this.instantiator = instantiator;
    }

    @SafeVarargs
    @Override
    public final Self and(Self... builders) {
        return instantiator.apply(() -> Stream.of(builders).flatMap(b -> b.getPredicateFunc().get()));
    }

    @SafeVarargs
    @Override
    public final Self or(Self... builders) {
        return instantiator.apply(() -> Stream.of(builders).flatMap(b -> b.getPredicateFunc().get()));
    }

    @Override
    public Self not(Self builder) {
        return instantiator.apply(() -> builder.getPredicateFunc().get());
    }

    @Override
    public Self compare(SearchableModelField<? super M> modelField, Operator op, Object... value) {
        if (modelField.equals(RoleModel.SearchableFields.REALM_ID)) {
            // don't filter by realm, as the LDAP directory is specific to the realm already
            return instantiator.apply(() -> Stream.of((String) value[0]));
        } else {
            return instantiator.apply(Stream::empty);
        }
    }

    public Supplier<Stream<String>> getPredicateFunc() {
        return predicateFunc;
    }

    public LdapModelCriteriaBuilderForRealm(Function<Supplier<Stream<String>>, Self> instantiator,
                                            Supplier<Stream<String>> predicateFunc) {
        this.instantiator = instantiator;
        this.predicateFunc = predicateFunc;
    }

}
