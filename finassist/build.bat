@echo off
rem Compiles the project and runs the self-checks. Needs a JDK 11+ on PATH.
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt || exit /b 1
del sources.txt
java -ea -cp out finassist.test.SelfTest
echo Main-Class: finassist.AgentCLI> out\manifest.txt
jar cfm Agent.jar out\manifest.txt -C out finassist
echo Built Agent.jar
