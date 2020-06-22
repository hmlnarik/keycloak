package org.keycloak.models.map.common;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractMapProviderFactory<T extends Provider> implements ProviderFactory<T> {

    public static final String PROVIDER_ID = "map";

    protected final Logger LOG = Logger.getLogger(getClass());

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
