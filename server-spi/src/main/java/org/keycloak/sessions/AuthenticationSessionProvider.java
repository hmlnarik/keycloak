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

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AuthenticationSessionProvider extends Provider {

    // Generates random ID
    AuthenticationSessionModel createAuthenticationSession(RealmModel realm, ClientModel client);

    AuthenticationSessionModel createAuthenticationSession(String id, RealmModel realm, ClientModel client);

    AuthenticationSessionModel getAuthenticationSession(RealmModel realm, String authenticationSessionId);

    void removeAuthenticationSession(RealmModel realm, AuthenticationSessionModel authenticationSession);

    // TODO: test and add to scheduler
    void removeExpired(RealmModel realm);
    void onRealmRemoved(RealmModel realm);
    void onClientRemoved(RealmModel realm, ClientModel client);


}
