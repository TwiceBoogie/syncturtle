#!/bin/sh

set -eu

LAUNCHER="org.springframework.boot.loader.launch.JarLauncher"

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
    echo "======================================================"
    echo "Running Syncturtle application setup"
    echo "======================================================"

    java "${LAUNCHER}" \
        --spring.profiles.active="${SETUP_PROFILES}" \
        --spring.main.web-application-type=none

    echo "======================================================"
    echo "Syncturtle application setup completed successfully"
    echo "======================================================"
}

start_application() {
    echo "======================================================"
    echo "Starting Syncturtle application"
    echo "Profiles: ${RUNTIME_PROFILES}"
    echo "======================================================"
    # exec replaces the shell with java so that it can receive container termination signals
    exec java "${LAUNCHER}" "$@"
}

if is_true "${SYNCTURTLE_AUTO_SETUP:-false}"; then
    run_setup
fi

start_application "$@"