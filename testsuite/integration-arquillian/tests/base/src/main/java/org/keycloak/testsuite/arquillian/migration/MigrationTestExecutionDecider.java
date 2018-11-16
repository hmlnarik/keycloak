/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.arquillian.migration;

import org.jboss.arquillian.test.spi.execution.ExecutionDecision;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;

import java.lang.reflect.Method;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 * @author tkyjovsk (refactoring)
 */
public class MigrationTestExecutionDecider implements TestExecutionDecider {

    private final Logger log = Logger.getLogger(MigrationTestExecutionDecider.class);
    private static final String MIGRATED_AUTH_SERVER_VERSION_PROPERTY = "migrated.auth.server.version";

    @Override
    public ExecutionDecision decide(Method method) {

        String migratedAuthServerVersion = System.getProperty(MIGRATED_AUTH_SERVER_VERSION_PROPERTY);
        boolean migrationTest = migratedAuthServerVersion != null;
        Migration migrationAnnotation = method.getAnnotation(Migration.class);
        
         if (migrationTest && migrationAnnotation != null) {
            ComparableVersion cvMigratedAuthServerVersion = new ComparableVersion(migratedAuthServerVersion);
            log.info("migration from version: " + cvMigratedAuthServerVersion);
            
            ComparableVersion cvVersionFrom = new ComparableVersion(migrationAnnotation.versionFrom());
            ComparableVersion cvVersionFromUpperBound =
              migrationAnnotation.versionFromUpperBound().isEmpty()
              ? cvVersionFrom
              : new ComparableVersion(migrationAnnotation.versionFromUpperBound());
            if (cvVersionFrom.compareTo(cvMigratedAuthServerVersion) >= 0 && cvVersionFromUpperBound.compareTo(cvMigratedAuthServerVersion) <= 0) {
                return ExecutionDecision.execute();
            } else {
                return ExecutionDecision.dontExecute(method.getName() + "doesn't fit with migration version.");
            }
        }
        if ((migrationTest && migrationAnnotation == null) || (!migrationTest && migrationAnnotation != null)) {
            return ExecutionDecision.dontExecute("Migration test and no migration annotation or no migration test and migration annotation");
        }
        return ExecutionDecision.execute();
    }

    @Override
    public int precedence() {
        return 2;
    }

}
