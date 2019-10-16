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

package org.keycloak.services.util;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.urls.UrlType;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResolveRelative {
    public static String resolveRelativeUri(KeycloakSession session, String rootUrl, String url) {
        if (url == null || !url.startsWith("/")) {
            return url;
        } else if (rootUrl != null) {
            return resolveRootUrl(session, rootUrl) + url;
        } else {
            return session.getContext().getUri().getBaseUriBuilder().path(url).build().toString();
        }
    }

    public static String resolveRootUrl(KeycloakSession session, String rootUrl) {
        if (rootUrl != null) {
            if (rootUrl.equals(Constants.AUTH_BASE_URL_PROP)) {
                rootUrl = session.getContext().getUri(UrlType.FRONTEND).getBaseUri().toString();
                if (rootUrl.endsWith("/")) {
                    rootUrl = rootUrl.substring(0, rootUrl.length() - 1);
                }
            } else if (rootUrl.equals(Constants.AUTH_ADMIN_URL_PROP)) {
                rootUrl = session.getContext().getUri(UrlType.ADMIN).getBaseUri().toString();
                if (rootUrl.endsWith("/")) {
                    rootUrl = rootUrl.substring(0, rootUrl.length() - 1);
                }
            }
        }
        return rootUrl;
    }
}
