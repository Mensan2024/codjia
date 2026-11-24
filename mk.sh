#!/bin/sh
set -e
cd "$(dirname "$0")"
L=/opt/gradle/lib
./kc.sh -classpath $L/kotlin-stdlib-2.0.21.jar -d build/core.jar engine-core/src/main/kotlin 2>&1 | grep -vE "JAVA_TOOL|_JAVA_OPTIONS" || true
./kc.sh -classpath $L/kotlin-stdlib-2.0.21.jar:build/core.jar -d build/jvm.jar engine-jvm/src/main/kotlin 2>&1 | grep -vE "JAVA_TOOL|_JAVA_OPTIONS" || true
