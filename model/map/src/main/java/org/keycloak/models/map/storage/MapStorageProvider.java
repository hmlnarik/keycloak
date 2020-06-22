package org.keycloak.models.map.storage;

import org.keycloak.models.map.common.HasUpdated;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

/**
 *
 * @author hmlnarik
 */
public interface MapStorageProvider extends Provider, ProviderFactory<MapStorageProvider> {
    
    public enum Flag {
        INITIALIZE_EMPTY,
        LOCAL
    }

    /**
     * Returns a key-value storage
     * @param <K> type of the primary key
     * @param <V> type of the value
     * @param name Name of the storage
     * @param flags
     * @return
     */
    <K, V extends HasUpdated<K>> MapStorage<K, V> getStorage(String name, Class<K> keyType, Class<V> valueType, Flag... flags);
}
