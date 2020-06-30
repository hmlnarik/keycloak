package org.keycloak.models.map.common;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractMapProviderFactory<T extends Provider, K, V extends HasUpdated<K>> implements ProviderFactory<T> {

    public static final String PROVIDER_ID = "map";

    protected final Logger LOG = Logger.getLogger(getClass());

    protected String configFileName;

    private final Class<V[]> valueArrayType;

    protected final ConcurrentMap<K, V> store = new ConcurrentHashMap<>();

    public AbstractMapProviderFactory(Class<V[]> valueType) {
        this.valueArrayType = valueType;
    }

    @Override
    public void init(Scope config) {
        // To survive server restarts, import records
        this.configFileName = config.get("file", "target/map-" + getClass().getSimpleName() + ".json");

        final File f = new File(configFileName);

        if (f.exists()) {
            LOG.debugf("Restoring clients from %s", configFileName);
            try {
                V[] values = Serialization.MAPPER.readValue(f, valueArrayType);
                Arrays.asList(values).forEach((V mce) -> store.put(mce.getId(), mce));
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
                Serialization.MAPPER.writeValue(f, store.values());
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
