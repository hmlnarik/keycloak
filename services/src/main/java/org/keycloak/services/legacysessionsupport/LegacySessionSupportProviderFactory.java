package org.keycloak.services.legacysessionsupport;

import org.keycloak.models.RealmProvider;
import org.keycloak.provider.ProviderFactory;

/**
 * @author Alexander Schwartz
 */
public interface LegacySessionSupportProviderFactory<T extends LegacySessionSupportProvider> extends ProviderFactory<T> {
}
