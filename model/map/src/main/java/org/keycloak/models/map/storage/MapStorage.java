package org.keycloak.models.map.storage;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author hmlnarik
 */
public interface MapStorage<K, V> {

    V get(K key);

    V put(K key, V value);

    V putIfAbsent(K key, V value);

    V remove(K key);

    V replace(K key, V value);

    Set<K> keySet();

    Set<Map.Entry<K,V>> entrySet();

    Collection<V> values();

}
