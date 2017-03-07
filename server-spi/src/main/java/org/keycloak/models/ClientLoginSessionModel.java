/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientLoginSessionModel {

    // TODO: Remove timestamp and not require updating it every refresh
    public int getTimestamp();
    public void setTimestamp(int timestamp);

    public Set<String> getRoles();
    public void setRoles(Set<String> roles);

    public Set<String> getProtocolMappers();
    public void setProtocolMappers(Set<String> protocolMappers);

    public String getNote(String name);
    public void setNote(String name, String value);
    public void removeNote(String name);
    public Map<String, String> getNotes();

    // TODO: Maybe remove...
    String getId();
    ClientModel getClient();
    RealmModel getRealm();
}
