all: compile

compile: Proposal.java Acceptor.java Proposer.java PaxosServer.java
	javac -d classfiles $^

paxos: compile
	java -classpath classfiles PaxosServer