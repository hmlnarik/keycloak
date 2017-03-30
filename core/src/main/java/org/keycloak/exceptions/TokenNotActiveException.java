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
package org.keycloak.exceptions;

import org.keycloak.common.VerificationException;
import org.keycloak.representations.JsonWebToken;

/**
 * Exception thrown for cases when token is invalid due to time constraints (expired, or not yet valid).
 * Cf. {@link JsonWebToken#isActive()}.
 * @author hmlnarik
 */
public class TokenNotActiveException extends VerificationException {

    private final JsonWebToken token;

    public TokenNotActiveException(JsonWebToken token) {
        this.token = token;
    }

    public TokenNotActiveException(JsonWebToken token, String message) {
        super(message);
        this.token = token;
    }

    public TokenNotActiveException(JsonWebToken token, String message, Throwable cause) {
        super(message, cause);
        this.token = token;
    }

    public TokenNotActiveException(JsonWebToken token, Throwable cause) {
        super(cause);
        this.token = token;
    }

    public JsonWebToken getToken() {
        return token;
    }

}
