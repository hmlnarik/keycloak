package org.keycloak.models.map.client;

import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientProviderFactory;
import org.keycloak.models.KeycloakSession;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author hmlnarik
 */
public class MapClientProviderFactory extends AbstractMapProviderFactory<ClientProvider, UUID, MapClientEntity> implements ClientProviderFactory {

    private final ConcurrentMap<UUID, ConcurrentMap<String, Integer>> REGISTERED_NODES_STORE = new ConcurrentHashMap<>();

    public MapClientProviderFactory() {
        super(MapClientEntity[].class);
    }

    @Override
    public ClientProvider create(KeycloakSession session) {
        return new MapClientProvider(session, store, REGISTERED_NODES_STORE);
    }
}
