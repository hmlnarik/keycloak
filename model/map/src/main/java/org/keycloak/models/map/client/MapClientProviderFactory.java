/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.models.map.client;

import org.keycloak.Config.Scope;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.utils.Serialization;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class MapClientProviderFactory implements ClientProviderFactory {

    public static final String PROVIDER_ID = "map";

    private static final Logger LOG = Logger.getLogger(MapClientProviderFactory.class);

    // must be static to survive server restarts
    private final ConcurrentMap<UUID, MapClientEntity> CLIENT_STORE = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<String, Integer>> REGISTERED_NODES_STORE = new ConcurrentHashMap<>();
    private String configFileName;

    @Override
    public ClientProvider create(KeycloakSession session) {
        return new MapClientProvider(session, CLIENT_STORE, REGISTERED_NODES_STORE);
    }

    @Override
    public void init(Scope config) {
        // To survive server restarts, import clients
        this.configFileName = config.get("file", "target/map-clients.json");

        final File f = new File(configFileName);
        if (f.exists()) {
            LOG.debugf("Restoring clients from %s", configFileName);
            try {
                MapClientEntity[] values = Serialization.MAPPER.readValue(f, MapClientEntity[].class);
                Arrays.asList(values).forEach(mce -> CLIENT_STORE.put(mce.getId(), mce));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        String fileName = this.configFileName;

        if (this.configFileName != null) {
            final File f = new File(fileName);
            LOG.debugf("Storing clients to %s", fileName);
            try {
                Serialization.MAPPER.writeValue(f, CLIENT_STORE.values());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
