public class ElectionTests {
    public static void main(String[] args) {

        System.out.println("Running automated tests.");

        Thread serverThread = new Thread(() -> {
            String[] serverArgs = { "testoutputs/server.txt" };
            PaxosServer.main(serverArgs);
        });

        testWithImmediateResponses();
    }

    private static void testWithImmediateResponses() {
        String electionManagerOutputFileName = "testoutputs/election_1.txt";
        
        String[] args = {"true", electionManagerOutputFileName};
        ElectionManager.main(args);

    }

    private static void testWithDelayedAndAbsentResponses() {
        
    }
}
