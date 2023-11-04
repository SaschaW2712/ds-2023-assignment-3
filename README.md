To compile the project, run `make`

-----------

RUNNING SERVER AND ELECTION 

To start a Paxos Server:
    - With default arguments: `make paxos`
    - With custom arguments: compile with `make`, then `java -classpath classfiles PaxosServer <output file> <port>`
        - Output file is optional (default is System.out).
            - Must include output file if using custom port
            - E.g. `java -classpath classfiles PaxosServer outputfile.txt`
        - Port is a port number, and is optional (default is 4567). If you add a port here, ensure the Election Manager is also started with that port
            - E.g. `java -classpath classfiles PaxosServer outputfile.txt 4568`
    - Multiple elections can be run consecutively (though not in parallel) without restarting the Paxos Server, as it clears its data after each election finishes

To start an Election Manager (which runs an election):
    - Must have a Paxos Server running
    - With default arguments: `make election`
    - With custom arguments: compile with `make`, then `java -classpath classfiles ElectionManager <use immediate responses?> <output file> <port>`
        - `use immediate responses?` is a true/false value that overrides member delays if set to true. It is optional.
            - Must include this value (can be "false") if using custom output file or port
            - E.g. `java -classpath classfiles ElectionManager true`
        - Output file is optional (default is System.out).
            - Must include output file if using custom port
            - E.g. `java -classpath classfiles ElectionManager false outputfile.txt`
        - Port is a port number, and is optional (default is 4567). If you add a port here, ensure the Paxos Server has also been started with that port
            - E.g. `java -classpath classfiles ElectionManager false outputfile.txt 4568`


-----------

AUTOMATED TESTING

Run automated tests with `make test`.

Organisation of test files:
    - Observed outputs for the Paxos Server are in `testoutputs/server.txt`
    - Observed outputs for the Election Manager (which includes all Members' output) are in `testoutputs/election_<test number>.txt`
        - E.g. `election_1.txt`

The cases covered by automated testing are outlined below:
    - Paxos works when all three proposers send a request at the same time
    - Paxos works when all members M1-M9 respond immediately and reliably (i.e. no delays or offline members)
    - Paxos works when members have independent profiles determining whether they will respond to a given query, and with what delay (implementation details of member profiles are outlined under `MEMBER PROFILES`)

-----------

MEMBER PROFILES
    - Members' responsiveness to requests is defined by a list of locations they could be at, with an indicator of how often they are there - represented by the `LocationWithRegularity` data class.
    - Each `Acceptor` has a `List<LocationWithRegularity>` in their attributes that represents this.
    - Every time an Acceptor receives a Prepare or Accept request, they find their "current location", and respond according to the `Location`'s details.
        - A `Location` has an internet speed and a response likelihood, affecting the speed and likelihood of their response respectively.
        - Random seeded numbers are used to calculate whether a member responds based on this - if they don't respond, they just close their output without writing anything to it.
        - They also wait for a period of time (from 0-2000 milliseconds) before responding, depending on their internet speed.
    - It would be unrealistic to use the actual timeframe of hours over which this vote would be happening, so the lengths of delays are roughly equivalent to 1 second = 1 hour.

-----------

KNOWN ISSUES

There were some bugs I noticed but was unable to replicate after extensive further testing, due to parallel threads making ordering of operations volatile.
If any of these appear, please exit the PaxosServer and ElectionManager processes (with CTRL-C) and retry them.

These issues appear to be less common when running automated tests - running `make paxos` and `make election` independently almost never trigger the problem.

    - Sometimes threads refuse to be interrupted when an election finishes, causing remaining Proposers to continue to try to get proposals passed despite only a couple of Acceptors still being connected.
    - Sometimes when many Acceptors write to their Paxos Server socket's output streams concurrently, only a few of the outputs will be received by the server's input streams.
        - I believe this could be an issue with the thread pool not having enough capacity available to service all the simultaneous events, but was unsuccessful with attempts to mitigate that.
        - I've made extensive use of synchronized blocks to avoid simultaneous writes, but this didn't solve the problem.