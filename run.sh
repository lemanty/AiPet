#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out/main
find src/main/java -name '*.java' > out/sources.txt
javac --release 21 -encoding UTF-8 -d out/main @out/sources.txt
java -cp out/main com.lemanty.aipet.App
