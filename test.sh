#!/bin/sh
# Compile et lance les 6 suites de tests sur la JVM (sans Gradle ni réseau).
set -e
cd "$(dirname "$0")"
./mk.sh
L=/opt/gradle/lib
# Une erreur de compilation (code ou tests) arrête tout : sinon les tests tourneraient sur d'anciens fichiers.
fail_on_error() { out=$(cat); echo "$out" | grep -vE "JAVA_TOOL|_JAVA_OPTIONS" || true; if echo "$out" | grep -q "error:"; then echo "ÉCHEC DE COMPILATION" >&2; exit 1; fi; }
J=$L/junit-4.13.2.jar:$L/hamcrest-core-1.3.jar
./kc.sh -classpath $L/kotlin-stdlib-2.0.21.jar:build/core.jar:$J -d build/core-test.jar engine-core/src/test/kotlin 2>&1 | fail_on_error
./kc.sh -classpath $L/kotlin-stdlib-2.0.21.jar:build/core.jar:build/jvm.jar:$J -d build/jvm-test.jar engine-jvm/src/test/kotlin 2>&1 | fail_on_error
P=io.github.mensan2024.itmentor.engine
java -Ditm.fonts=engine-android/src/main/assets/fonts -cp $L/kotlin-stdlib-2.0.21.jar:build/core.jar:build/jvm.jar:build/core-test.jar:build/jvm-test.jar:$J \
  org.junit.runner.JUnitCore $P.TimelineTest $P.EasingAndAnimationTest $P.TextLayoutTest $P.PathTest $P.FormatTest $P.TemplateTest $P.DomainTest $P.MusicTest $P.PlannerTest $P.VariationTest $P.SecurityTest $P.NarrationTest $P.jvm.MusicCatalogTest $P.jvm.Mp4MuxerTest $P.jvm.RenderTest $P.jvm.UnderstandingTest $P.jvm.ScenarioTest $P.jvm.CorrectionTest $P.jvm.StudioTest 2>&1 | grep -vE "JAVA_TOOL|_JAVA_OPTIONS"
