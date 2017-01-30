#!/bin/bash -ex

run-adapter-tests() {
    APP_SRV_PROFILE="${1:-wildfly}"
    AUTH_SRV_PROFILE="${2:-wildfly}"

    APP_SRV_PREPARE="prepare-appsrv-build-$APP_SRV_PROFILE"
    eval "if declare -f $APP_SRV_PREPARE > /dev/null ; then $APP_SRV_PREPARE ; fi"
    AUTH_SRV_PREPARE="prepare-authsrv-build-$AUTH_SRV_PROFILE"
    eval "if declare -f $AUTH_SRV_PREPARE > /dev/null ; then $AUTH_SRV_PREPARE ; fi"

    mvn install --no-snapshot-updates -B -DskipTests \
      -f "testsuite/integration-arquillian/servers" \
      -Pauth-server-$AUTH_SRV_PROFILE,app-server-$APP_SRV_PROFILE \
      $ADDITIONAL_MVN_PARAMS

#   Debugging tomcat: every 3 seconds, show the thread dump
#    ( while : ; do sleep 3 ; jps -v ; jps -v | grep Bootstrap | cut -f 1 -d \  | xargs --no-run-if-empty kill -3 ; done )&
#    A_PID=$?

    APP_SRV_PREPARE="prepare-appsrv-$APP_SRV_PROFILE"
    eval "if declare -f $APP_SRV_PREPARE > /dev/null ; then $APP_SRV_PREPARE ; fi"
    AUTH_SRV_PREPARE="prepare-authsrv-$AUTH_SRV_PROFILE"
    eval "if declare -f $AUTH_SRV_PREPARE > /dev/null ; then $AUTH_SRV_PREPARE ; fi"

    mvn clean test --no-snapshot-updates -B \
      -f "testsuite/integration-arquillian/tests/other/adapters" \
      -Djava.security.egd=file:/dev/urandom \
      -Pauth-server-$AUTH_SRV_PROFILE,app-server-$APP_SRV_PROFILE \
      $ADDITIONAL_MVN_PARAMS "${@:3}"

#    kill $A_PID
}

prepare-appsrv-tomcat8() {
    PROPS=`find testsuite/integration-arquillian/tests/other/adapters/tomcat -name catalina.properties`
    for P in $PROPS; do
        sed -i -e 's/tomcat.util.scan.StandardJarScanFilter.jarsToSkip=/tomcat.util.scan.StandardJarScanFilter.jarsToSkip=bcprov*.jar,/' "$P"
        echo "==== $P ===="
        cat "$P"
        echo "==== End of $P ===="
    done
}

prepare-appsrv-tomcat9() {
    # Enable logging:
    #    cat <<EOT >> testsuite/integration-arquillian/tests/other/adapters/tomcat/tomcat9/target/containers/app-server-tomcat9/conf/logging.properties
#org.apache.catalina.util.LifecycleBase.level = FINE
#org.apache.catalina.startup.level = FINE
#EOT

    PROPS=`find testsuite/integration-arquillian/tests/other/adapters/tomcat -name catalina.properties`
    for P in $PROPS; do
        sed -i -e 's/tomcat.util.scan.StandardJarScanFilter.jarsToSkip=/tomcat.util.scan.StandardJarScanFilter.jarsToSkip=bcprov*.jar,/' "$P"
        echo "==== $P ===="
        cat "$P"
        echo "==== End of $P ===="
    done
}

prepare-appsrv-build-fuse63() {
    FUSE_VERSION=6.3.0.redhat-239

    if ! [ -f $HOME/.m2/repository/org/jboss/fuse/jboss-fuse-karaf/$FUSE_VERSION/jboss-fuse-karaf-$FUSE_VERSION.zip ]; then
        [ -d Download ] || mkdir Download
        wget --quiet \
           http://origin-repository.jboss.org/nexus/content/groups/m2-proxy/org/jboss/fuse/jboss-fuse-karaf/$FUSE_VERSION/jboss-fuse-karaf-$FUSE_VERSION.zip \
           -O Download/jboss-fuse-karaf-$FUSE_VERSION.zip
        mvn install:install-file \
         -DgroupId=org.jboss.fuse \
         -DartifactId=jboss-fuse-karaf \
         -Dversion=$FUSE_VERSION \
         -Dpackaging=zip \
         -Dfile=Download/jboss-fuse-karaf-$FUSE_VERSION.zip
    fi

    export ADDITIONAL_MVN_PARAMS="$ADDITIONAL_MVN_PARAMS
      -Dfuse63.version=$FUSE_VERSION -Dapp.server.karaf.update.config=true
      -Dmaven.local.settings=$HOME/.m2/settings.xml
      -Drepositories=,http://repository.ops4j.org \
      -Dmaven.repo.local=$HOME/.m2/repository"
}

case "$1" in
    "old")
        mvn test -B -f testsuite/integration
        mvn test -B -f testsuite/jetty
        mvn test -B -f testsuite/tomcat6
        mvn test -B -f testsuite/tomcat7
        mvn test -B -f testsuite/tomcat8
        ;;

    "group1")
        cd testsuite/integration-arquillian/tests/base
        mvn test -B -Dtest=org.keycloak.testsuite.ad*.**.*Test
        ;;

    "group2")
        cd testsuite/integration-arquillian/tests/base
        mvn test -B -Dtest=org.keycloak.testsuite.ac*.**.*Test,org.keycloak.testsuite.b*.**.*Test,org.keycloak.testsuite.cli*.**.*Test,org.keycloak.testsuite.co*.**.*Test
        ;;

    "group3")
        cd testsuite/integration-arquillian/tests/base
        mvn test -B -Dtest=org.keycloak.testsuite.d*.**.*Test,org.keycloak.testsuite.e*.**.*Test,org.keycloak.testsuite.f*.**.*Test,org.keycloak.testsuite.i*.**.*Test
        ;;

    "group4")
        cd testsuite/integration-arquillian/tests/base
        mvn test -B -Dtest=org.keycloak.testsuite.k*.**.*Test,org.keycloak.testsuite.m*.**.*Test,org.keycloak.testsuite.o*.**.*Test
        ;;

    "adapter")
        cd testsuite/integration-arquillian/tests/other/adapters
        mvn test -B
        ;;

    adapter-arquillian)
        run-adapter-tests "$2" "$3" -Dtest='!%regex[.*example.*]'
        ;;

    adapter-examples-arquillian)
        run-adapter-tests "$2" "$3" -Dtest='%regex[.*example.*]'
        ;;

    "console")
        cd testsuite/integration-arquillian/tests/other/console
        mvn test -B
        ;;

esac
