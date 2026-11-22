#!/usr/bin/env bash
# Compile and run without Maven. Put sqlite-jdbc and slf4j-api jars in lib/ first (see README).
# Usage: ./run.sh            -> GUI
#        ./run.sh --console 30 -> headless demo for 30 s
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/main/java/elevator/sim/*.java
java -cp "out:lib/*" elevator.sim.Main "$@"
