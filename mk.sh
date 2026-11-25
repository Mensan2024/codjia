#!/bin/sh
set -e
cd "$(dirname "$0")"
fail_on_error() { out=$(cat); echo "$out" | grep -vE "JAVA_TOOL|_JAVA_OPTIONS" || true; if echo "$out" | grep -q "error:"; then echo "ÉCHEC DE COMPILATION" >&2; exit 1; fi; }
L=/opt/gradle/lib
./kc.sh -classpath $L/kotlin-stdlib-2.0.21.jar -d build/core.jar engine-core/src/main/kotlin 2>&1 | fail_on_error
./kc.sh -classpath $L/kotlin-stdlib-2.0.21.jar:build/core.jar -d build/jvm.jar engine-jvm/src/main/kotlin 2>&1 | fail_on_error
