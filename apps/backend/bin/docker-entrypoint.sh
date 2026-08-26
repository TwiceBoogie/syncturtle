#!/bin/sh

set -eu

APPLICATION_JAR=/app/application.jar
RUNTIME_PROFILES="${SPRING_PROFILES_ACTIVE:-docker}"
SETUP_PROFILES="${SYNCTURTLE_SETUP_PROFILES:-setup,${RUNTIME_PROFILES}}"

is_true() {
    case "${1:-false}" in
        true | TRUE | True | 1 | yes | YES | Yes)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

run_setup() {
    echo "Running SyncTurtle application setup with profiles ${SETUP_PROFILES}"
    java -jar "${APPLICATION_JAR}" \
        --spring.profiles.active="${SETUP_PROFILES}" \
        --spring.main.web-application-type=none
}

if is_true "${SYNCTURTLE_AUTO_SETUP:-false}"; then
    run_setup
fi

echo "Starting SyncTurtle application with profiles ${RUNTIME_PROFILES}"
exec java -jar "${APPLICATION_JAR}" "$@"