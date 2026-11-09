.PHONY: all test run-gui run-cli clean
all test:
	sh build.sh
run-gui:
	java -cp out finassist.Main gui --demo
run-cli:
	java -cp out finassist.Main cli --demo
clean:
	rm -rf out
