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

package org.keycloak.sessions;

import java.util.Map;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;

/**
 * Predecesor of LoginSessionModel and Action tickets. Not sure if it's even needed...
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface CommonLoginSessionModel {

    public String getId();
    public RealmModel getRealm();
    public ClientModel getClient();

    public int getTimestamp();
    public void setTimestamp(int timestamp);

    public String getAction();
    public void setAction(String action);

    // TODO: Not needed here...?
    public Set<String> getRoles();
    public void setRoles(Set<String> roles);

    // TODO: Not needed here...?
    public Set<String> getProtocolMappers();
    public void setProtocolMappers(Set<String> protocolMappers);

    public String getNote(String name);
    public void setNote(String name, String value);
    public void removeNote(String name);
    public Map<String, String> getNotes();
}
