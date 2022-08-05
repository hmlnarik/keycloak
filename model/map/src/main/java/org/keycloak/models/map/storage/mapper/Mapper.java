/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.mapper;

import java.util.Map;

/**
 * Implementation of this interface represents a bidirectional mapper for a single value of type {@code F}
 * obtained from an (potentially multiple values of) object of type {@code R}, or set values from
 * the single value to an object of type {@code R}.
 * <p>
 * If the {@link #there} operation is not bijective, {@link #back} operation may not be
 * feasible and may be undefined. See {@link #back} for further details.
 * <p>
 * The idea and the relationship between the two methods is illustrated below:
 * <pre>
 *  ┌─────────┐                                                           ┌─────────┐
 *  │ Field 1 ├─┐                                                      ┌─►│ Field 1 │
 *  ├─────────┤ │                                                      │  ├─────────┤
 *  │ Field 2 │ │  ┌──────────┐                     ┌────────────────┐ │  │ Field 2 │
 *  ├─────────┤ └─►│          │                     │                ├─┘  ├─────────┤
 *  │ Field 3 ├───►│ there(e) ├──► value: Object ──►│ back(value, r) ├───►│ Field 3 │
 *  ├─────────┤ ┌─►│          │                     │                ├─┐  ├─────────┤
 *  │ Field 4 ├─┘  └──────────┘                     └────────────────┘ └─►│ Field 4 │
 *  ├─────────┤                                                           ├─────────┤
 *  │ ...     │                                                           │ ...     │
 *  └─────────┘                                                           └─────────┘
 *     e: R                                                                  r: R
 * </pre>
 *
 * @author hmlnarik
 */
public interface Mapper<R> {

    /**
     * Creates a value of type {code F} computed from the internals
     * of the {@code object} object of type {@code T}.
     * This may aggregate multiple fields from the {@code object}.
     * <p>
     * <pre>
     *  ┌─────────┐                         
     *  │ Field 1 ├─┐                       
     *  ├─────────┤ │                       
     *  │ Field 2 │ │  ┌───────────────┐         
     *  ├─────────┤ └─►│               │         
     *  │ Field 3 ├───►│ there(object) ├──► value
     *  ├─────────┤ ┌─►│               │         
     *  │ Field 4 ├─┘  └───────────────┘         
     *  ├─────────┤                         
     *  │ ...     │                         
     *  └─────────┘                         
     *     e: T                             
     * </pre>
     * 
     * @param object
     * @return Transformed value
     */
    public Object there(R object);

    // TODO: consider java.lang.reflect.Type rather than Class
    /**
     * Returns a type of the object returned by {@link #there} method.
     * @return 
     */
    public Class<?> getThereClass();

    /**
     * This methods serves to set the {@code object} properties from the {@code value}
     * obtained previously from the call to {@link #there}.
     * <p>
     * It is possible that the {@link #there} transformation is not bijective.
     * Then the behaviour of this function is undefined. It is desirable that
     * in such case this method should only set those fields that may be
     * reliably reconstructed from {@code value}, or no-op if no such reconstruction
     * is possible.
     * <p>
     * <pre>
     *                             ┌─────────┐
     *                          ┌─►│ Field 1 │
     *                          │  ├─────────┤
     *  ┌─────────────────────┐ │  │ Field 2 │
     *  │                     ├─┘  ├─────────┤
     *  │ back(object, value) ├───►│ Field 3 │
     *  │                     ├─┐  ├─────────┤
     *  └─────────────────────┘ └─►│ Field 4 │
     *                             ├─────────┤
     *                             │ ...     │
     *                             └─────────┘
     *                               object
     * </pre>
     *
     * @param object
     * @param value
     */
    void back(R object, Object value);

    Map<String, Object> backToMap(Object value);

}
