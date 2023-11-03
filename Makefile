all: compile

compile: $(wildcard *.java) $(wildcard enums/*.java) $(wildcard dataclasses/*.java)
	javac -d classfiles $^

paxos: compile
	java -classpath classfiles PaxosServer

election: compile
	java -classpath classfiles ElectionManager

test: compile
	java -classpath classfiles ElectionTests