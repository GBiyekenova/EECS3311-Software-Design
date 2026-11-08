#!/bin/sh
set -e
rm -rf out && mkdir out
javac -d out $(find src -name '*.java')
java -ea -cp out finassist.test.SelfTest
