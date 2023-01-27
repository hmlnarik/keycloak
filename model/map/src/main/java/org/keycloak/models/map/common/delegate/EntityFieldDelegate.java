package org.keycloak.models.map.common.delegate;

import java.util.Collection;
import java.util.Map;

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.UpdatableEntity;
import java.util.Collections;
import java.util.Set;

public interface EntityFieldDelegate<E> extends UpdatableEntity {

    public abstract class WithEntity<E extends UpdatableEntity> implements EntityFieldDelegate<E> {
        private final E entity;

        public WithEntity(E entity) {
            this.entity = entity;
        }

        @Override
        public <EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object get(EF field) {
            return field.get(entity);
        }

        @Override
        public <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void set(EF field, T value) {
            field.set(entity, value);
        }

        @Override
        public boolean isUpdated() {
            return entity.isUpdated();
        }
    }

    // Non-collection values
    <EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object get(EF field);
    default <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void set(EF field, T value) {}

    default <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void collectionAdd(EF field, T value) {
        @SuppressWarnings("unchecked")
        Collection<T> c = (Collection<T>) get(field);
        if (c == null) {
            set(field, field.getFieldClass().isAssignableFrom(Set.class) ? Collections.singleton(value) : Collections.singletonList(value));
        } else {
            c.add(value);
        }
    }
    default <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object collectionRemove(EF field, T value) {
        Collection<?> c = (Collection<?>) get(field);
        return c == null ? null : c.remove(value);
    }

    /**
     * 
     * @param <K> Key type
     * @param <T> Value type
     * @param field Field identifier. Should be one of the generated {@code *Fields} enum constants.
     * @param key Key
     * @param valueClass class of the value
     * @return
     */
    default <K, EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object mapGet(EF field, K key) {
        @SuppressWarnings("unchecked")
        Map<K, ?> m = (Map<K, ?>) get(field);
        return m == null ? null : m.get(key);
    }
    default <K, T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void mapPut(EF field, K key, T value) {
        @SuppressWarnings("unchecked")
        Map<K, T> m = (Map<K, T>) get(field);
        if (m == null) {
            set(field, Collections.singletonMap(key, value));
        } else {
            m.put(key, value);
        }
    }
    default <K, EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object mapRemove(EF field, K key) {
        @SuppressWarnings("unchecked")
        Map<K, ?> m = (Map<K, ?>) get(field);
        if (m != null) {
            return m.remove(key);
        }
        return null;
    }

}
