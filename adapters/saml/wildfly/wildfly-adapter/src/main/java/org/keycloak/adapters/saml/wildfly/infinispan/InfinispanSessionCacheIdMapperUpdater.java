/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adapters.saml.wildfly.infinispan;

import org.keycloak.adapters.spi.SessionIdMapper;

import io.undertow.servlet.api.DeploymentInfo;
import java.util.*;
import java.util.concurrent.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.*;
import org.infinispan.notifications.cachelistener.event.*;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
@Listener
public class InfinispanSessionCacheIdMapperUpdater {

    private static final Logger LOG = Logger.getLogger(InfinispanSessionCacheIdMapperUpdater.class);

    public static final String DEFAULT_CACHE_CONTAINER_JNDI_NAME = "java:jboss/infinispan/container/web";

    private static final String DEPLOYMENT_CACHE_CONTAINER_JNDI_NAME_PARAM_NAME = "keycloak.sessionIdMapperUpdater.infinispan.cacheContainerJndi";
    private static final String DEPLOYMENT_CACHE_NAME_PARAM_NAME = "keycloak.sessionIdMapperUpdater.infinispan.cacheName";

    private final ConcurrentMap<String, Queue<Event>> map = new ConcurrentHashMap<>();

    private final SessionIdMapper idMapper;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public InfinispanSessionCacheIdMapperUpdater(SessionIdMapper idMapper) {
        this.idMapper = idMapper;
    }

    @TransactionRegistered
    public void startTransaction(TransactionRegisteredEvent event) {
        map.put(event.getGlobalTransaction().globalId(), new ConcurrentLinkedQueue<Event>());
    }

    @CacheStarted
    public void cacheStarted(CacheStartedEvent event) {
        this.executor = Executors.newSingleThreadExecutor();
    }

    @CacheStopped
    public void cacheStopped(CacheStoppedEvent event) {
        this.executor.shutdownNow();
    }

    @CacheEntryCreated
    @CacheEntryInvalidated
    @CacheEntryModified
    @CacheEntryRemoved
    public void addEvent(TransactionalEvent event) {
        map.get(event.getGlobalTransaction().globalId()).add(event);
    }

    @TransactionCompleted
    public void endTransaction(TransactionCompletedEvent event) {
        Queue<Event> events = map.remove(event.getGlobalTransaction().globalId());

        if (events == null || ! event.isTransactionSuccessful()) {
            return;
        }

        if (event.isOriginLocal()) {
            // Local events are processed by IdMapperUpdaterSessionListener
            return;
        }

        for (final Event e : events) {
            switch (e.getType()) {
                case CACHE_ENTRY_CREATED:
                    this.executor.submit(new Runnable() {
                        @Override public void run() {
                            cacheEntryCreated((CacheEntryCreatedEvent) e); 
                        }
                    });
                    break;

                case CACHE_ENTRY_REMOVED:
                    this.executor.submit(new Runnable() {
                        @Override public void run() {
                            cacheEntryRemoved((CacheEntryCreatedEvent) e);
                        }
                    });
                    break;

                case CACHE_ENTRY_INVALIDATED:
                    this.executor.submit(new Runnable() {
                        @Override public void run() {
                            cacheEntryInvalidated((CacheEntryCreatedEvent) e);
                        }
                    });
                    break;

                case CACHE_ENTRY_MODIFIED:
                    this.executor.submit(new Runnable() {
                        @Override public void run() {
                            cacheEntryModified((CacheEntryCreatedEvent) e);
                        }
                    });
                    break;
            }
        }
    }

    private void cacheEntryCreated(CacheEntryCreatedEvent event) {
        // the event.key is instance of SessionAttributesKey [1], and event.value is instance
        // of e.g. SimpleMarshalledValue [2]. Both the classes are implementation-specific
        // [1] https://github.com/wildfly/wildfly/blob/master/clustering/web/infinispan/src/main/java/org/wildfly/clustering/web/infinispan/session/coarse/SessionAttributesKey.java
        // [2] https://github.com/wildfly/wildfly/blob/master/clustering/marshalling/jboss/src/main/java/org/wildfly/clustering/marshalling/jboss/SimpleMarshalledValue.java
        LOG.debugv("cacheEntryCreated {0}:{1}", event.getKey(), event.getValue());
    }

    private void cacheEntryRemoved(CacheEntryRemovedEvent event) {
        LOG.debugv("cacheEntryRemoved {0}:{1}", event.getKey(), event.getValue());
    }

    private void cacheEntryModified(CacheEntryModifiedEvent event) {
        LOG.debugv("cacheEntryModified {0}:{1}", event.getKey(), event.getValue());
    }

    private void cacheEntryInvalidated(CacheEntryInvalidatedEvent event) {
        LOG.debugv("cacheEntryInvalidated {0}:{1}", event.getKey(), event.getValue());
    }

    public static void addTokenStoreUpdaters(DeploymentInfo deploymentInfo, SessionIdMapper mapper) {
       boolean distributable = Objects.equals(
          deploymentInfo.getSessionManagerFactory().getClass().getName(),
          "org.wildfly.clustering.web.undertow.session.DistributableSessionManagerFactory"
        );

        if (! distributable) {
            LOG.warnv("Deployment {0} does not use supported distributed session cache mechanism", deploymentInfo.getDeploymentName());
            return;
        }

        Map<String, String> initParameters = deploymentInfo.getInitParameters();
        String cacheContainerLookup = (initParameters != null && initParameters.containsKey(DEPLOYMENT_CACHE_CONTAINER_JNDI_NAME_PARAM_NAME))
          ? initParameters.get(DEPLOYMENT_CACHE_CONTAINER_JNDI_NAME_PARAM_NAME)
          : DEFAULT_CACHE_CONTAINER_JNDI_NAME;
        String sessionCacheName = (initParameters != null && initParameters.containsKey(DEPLOYMENT_CACHE_NAME_PARAM_NAME))
          ? initParameters.get(DEPLOYMENT_CACHE_NAME_PARAM_NAME)
          : deploymentInfo.getDeploymentName();

        try {
            EmbeddedCacheManager cacheManager = (EmbeddedCacheManager) new InitialContext().lookup(cacheContainerLookup);
            Cache<Object, Object> cache = cacheManager.getCache(sessionCacheName, false);

            if (cache == null) {
                LOG.warnv("Failed to obtain distributed session cache, lookup={0}, cache name={1}", cacheContainerLookup, sessionCacheName);
                return;
            }

            cache.addListener(new InfinispanSessionCacheIdMapperUpdater(mapper));
            LOG.debugv("Added listener to distributed session cache, lookup={0}, cache name={1}", cacheContainerLookup, sessionCacheName);
        } catch (NamingException ex) {
            LOG.warnv("Failed to obtain distributed session cache container, lookup={0}", cacheContainerLookup);
        }
    }
}
