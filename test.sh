#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out/test
find src/main/java src/test/java -name '*.java' > out/test-sources.txt
javac --release 21 -encoding UTF-8 -d out/test @out/test-sources.txt
java -Djava.awt.headless=true -cp out/test com.lemanty.aipet.TaskManagerTest
java -Djava.awt.headless=true -cp out/test com.lemanty.aipet.WindowFrameBufferTest
