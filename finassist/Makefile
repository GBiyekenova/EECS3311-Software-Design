.PHONY: all test run-gui run-cli run-agent clean
all test:
	sh build.sh
run-gui:
	java -cp out finassist.Main gui --demo
run-cli:
	java -cp out finassist.Main cli --demo
clean:
	rm -rf out Agent.jar
run-agent:
	java -jar Agent.jar -input examples/task.txt -demo
