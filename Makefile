all: compile

compile: $(wildcard *.java) $(wildcard enums/*.java)
	javac -d classfiles $^

paxos: compile
	java -classpath classfiles PaxosServer