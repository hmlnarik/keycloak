/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication;

import org.keycloak.TokenVerifier;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.*;
import org.keycloak.models.*;
import org.keycloak.services.Urls;
import org.keycloak.sessions.LoginSessionModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;

/**
 * Representation of a token that represents a time-limited reset credentials action.
 * <p>
 * This implementation handles signature.
 *
 * @author hmlnarik
 */
public class ResetCredentialsActionToken extends DefaultActionToken {

    private static final Logger LOG = Logger.getLogger(ResetCredentialsActionToken.class);

    private static final String RESET_CREDENTIALS_ACTION = "reset-credentials";
    public static final String NOTE_LOGIN_SESSION_ID = "clientSessionId";
    private static final String JSON_FIELD_LOGIN_SESSION_ID = "lsid";
    private static final String JSON_FIELD_LAST_CHANGE_PASSWORD_TIMESTAMP = "lcpt";

    @JsonIgnore
    private LoginSessionModel loginSession;

    @JsonProperty(value = JSON_FIELD_LAST_CHANGE_PASSWORD_TIMESTAMP)
    private Long lastChangedPasswordTimestamp;

    public ResetCredentialsActionToken(String userId, int absoluteExpirationInSecs, UUID actionVerificationNonce, Long lastChangedPasswordTimestamp, String loginSessionId) {
        super(userId, RESET_CREDENTIALS_ACTION, absoluteExpirationInSecs, actionVerificationNonce);
        setNote(NOTE_LOGIN_SESSION_ID, loginSessionId);
        this.lastChangedPasswordTimestamp = lastChangedPasswordTimestamp;
    }

    public ResetCredentialsActionToken(String userId, int absoluteExpirationInSecs, UUID actionVerificationNonce, Long lastChangedPasswordTimestamp, LoginSessionModel loginSession) {
        this(userId, absoluteExpirationInSecs, actionVerificationNonce, lastChangedPasswordTimestamp, loginSession == null ? null : loginSession.getId());
        this.loginSession = loginSession;
    }

    private ResetCredentialsActionToken() {
        super(null, null, -1, null);
    }

    public LoginSessionModel getLoginSession() {
        return this.loginSession;
    }

    public void setLoginSession(LoginSessionModel loginSession) {
        this.loginSession = loginSession;
        setLoginSessionId(loginSession == null ? null : loginSession.getId());
    }

    @JsonProperty(value = JSON_FIELD_LOGIN_SESSION_ID)
    public String getLoginSessionId() {
        return getNote(NOTE_LOGIN_SESSION_ID);
    }

    public void setLoginSessionId(String loginSessionId) {
        setNote(NOTE_LOGIN_SESSION_ID, loginSessionId);
    }

    public Long getLastChangedPasswordTimestamp() {
        return lastChangedPasswordTimestamp;
    }

    public void setLastChangedPasswordTimestamp(Long lastChangedPasswordTimestamp) {
        this.lastChangedPasswordTimestamp = lastChangedPasswordTimestamp;
    }

    @Override
    @JsonIgnore
    public Map<String, String> getNotes() {
        Map<String, String> res = super.getNotes();
        if (this.loginSession != null) {
            res.put(NOTE_LOGIN_SESSION_ID, getNote(NOTE_LOGIN_SESSION_ID));
        }
        return res;
    }

    public String serialize(KeycloakSession session, RealmModel realm, UriInfo uri) {
        String issuerUri = getIssuer(realm, uri);
        KeyManager.ActiveHmacKey keys = session.keys().getActiveHmacKey(realm);

        this
          .issuedAt(Time.currentTime())
          .id(getActionVerificationNonce().toString())
          .issuer(issuerUri)
          .audience(issuerUri);

        return new JWSBuilder()
          .kid(keys.getKid())
          .jsonContent(this)
          .hmac512(keys.getSecretKey());
    }

    private static String getIssuer(RealmModel realm, UriInfo uri) {
        return Urls.realmIssuer(uri.getBaseUri(), realm.getName());
    }

    /**
     * Returns a {@code DefaultActionToken} instance decoded from the given string. If decoding fails, returns {@code null}
     *
     * @param session
     * @param actionTokenString
     * @return
     */
    public static ResetCredentialsActionToken deserialize(KeycloakSession session, RealmModel realm, UriInfo uri, String token,
      Predicate<? super ResetCredentialsActionToken>... checks) throws VerificationException {
        return TokenVerifier.create(token, ResetCredentialsActionToken.class)
          .secretKey(session.keys().getActiveHmacKey(realm).getSecretKey())
          .realmUrl(getIssuer(realm, uri))
          .tokenType(RESET_CREDENTIALS_ACTION)

          .checkActive(false)   // TODO: If this line is omitted, the following tests in ResetPasswordTest fail: resetPasswordExpiredCodeShort, resetPasswordExpiredCode

          .check(ACTION_TOKEN_BASIC_CHECKS)
          .check(checks)
          .verify()
          .getToken()
        ;
    }
}
