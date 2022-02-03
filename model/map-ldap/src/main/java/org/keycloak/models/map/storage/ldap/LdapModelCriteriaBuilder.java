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

import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.internal.AndCondition;
import org.keycloak.storage.ldap.idm.query.internal.NoopCondition;
import org.keycloak.storage.ldap.idm.query.internal.NotCondition;
import org.keycloak.storage.ldap.idm.query.internal.OrCondition;

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
public abstract class LdapModelCriteriaBuilder<E, M, Self extends LdapModelCriteriaBuilder<E, M, Self>> implements ModelCriteriaBuilder<M, Self> {

    private final Function<Supplier<Condition>, Self> instantiator;
    private Supplier<Condition> predicateFunc = null;

    public LdapModelCriteriaBuilder(Function<Supplier<Condition>, Self> instantiator) {
        this.instantiator = instantiator;
    }

    @SafeVarargs
    @Override
    public final Self and(Self... builders) {
        return instantiator.apply(() -> {
            Condition[] conditions = Stream.of(builders).map(b -> b.getPredicateFunc().get()).filter(condition -> !(condition instanceof NoopCondition)).toArray(Condition[]::new);
            return conditions.length > 0 ? new AndCondition(conditions) : new NoopCondition();
        });
    }

    @SafeVarargs
    @Override
    public final Self or(Self... builders) {
        return instantiator.apply(() -> {
            Condition[] conditions = Stream.of(builders).map(b -> b.getPredicateFunc().get()).filter(condition -> !(condition instanceof NoopCondition)).toArray(Condition[]::new);
            return conditions.length > 0 ? new OrCondition(conditions) : new NoopCondition();
        });
    }

    @Override
    public Self not(Self builder) {
        return instantiator.apply(() -> {
            Condition condition = builder.getPredicateFunc().get();
            return condition instanceof NoopCondition ? condition : new NotCondition(condition);
        });
    }

    public Supplier<Condition> getPredicateFunc() {
        return predicateFunc;
    }

    public LdapModelCriteriaBuilder(Function<Supplier<Condition>, Self> instantiator,
                                    Supplier<Condition> predicateFunc) {
        this.instantiator = instantiator;
        this.predicateFunc = predicateFunc;
    }

}
