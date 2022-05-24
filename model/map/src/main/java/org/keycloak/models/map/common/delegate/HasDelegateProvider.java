package org.keycloak.models.map.common.delegate;

public interface HasDelegateProvider<E> {
    DelegateProvider<E> getDelegateProvider();
}
