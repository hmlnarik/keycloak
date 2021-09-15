/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.tree;

import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;

/**
 * This interface represents a generalized characteristic function assigned with a node in a tree store
 * which indicates possibility of this storage containing objects which satisfy given {@link DefaultModelCriteria criteria}.
 * 
 * @author hmlnarik
 */
public interface AuthoritativeDecider {

    /**
     * Authoritative status is the status attributed to the given model criteria and storage that
     * represents qualitative chance of existence of the entities in the given storage which satisfy
     * the given criteria.
     */
    public enum AuthoritativeStatus { 
        /**
         * For the given criteria and store, there can exist no record which satisfies given criteria.
         * There might be other stores in the tree which would contain such a record though.
         * <p>
         * Example: If the tested criterion is that the domain of the user's email address is {@code example.com},
         * and the particular storage is the only one which stores users from {@code acme.org} email domain,
         * then this status applies.
         */
        AUTHORITATIVE_NO,
        /**
         * For the given criteria and store, there may exist zero or many record satisfying given criteria.
         * There might be other stores which are also {@code AUTHORITATIVE_MAYBE}.
         * <p>
         * Example: looking up a user record with {@code username == slartibartfast} may be valid for several stores.
         */
        AUTHORITATIVE_MAYBE,
        /**
         * For the given criteria and store, there may exist zero or many record satisfying given criteria.
         * There can be no other stores which would contain such records.
         * <p>
         * Example: If the criterium is domain of the user's email address and the particular storage is
         * the only one which stores users from @acme.org email domain, then this status applies.
         */
        AUTHORITATIVE_STRONGLY,

        UNKNOWN
    }
    
    public AuthoritativeStatus isAuthoritative(DefaultModelCriteria<?> criteria);



}
