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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author hmlnarik
 */
public class MapClientProviderFactory implements ClientProviderFactory {

    public static final String PROVIDER_ID = "map";

    // must be static to survive server restarts
    private static final ConcurrentMap<UUID, MapClientEntity> CLIENT_STORE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, ConcurrentMap<String, Integer>> REGISTERED_NODES_STORE = new ConcurrentHashMap<>();

    @Override
    public ClientProvider create(KeycloakSession session) {
        return new MapClientProvider(session, CLIENT_STORE, REGISTERED_NODES_STORE);
    }

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
