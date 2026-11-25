#!/bin/sh
# Compiles src/ into out/, runs the self-checks and builds the standalone Agent.jar.
set -e
rm -rf out && mkdir out
javac -d out $(find src -name '*.java')
java -ea -cp out finassist.test.SelfTest
printf 'Main-Class: finassist.AgentCLI\n' > out/manifest.txt
jar cfm Agent.jar out/manifest.txt -C out finassist
echo "Built Agent.jar"
