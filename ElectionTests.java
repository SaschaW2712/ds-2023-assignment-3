public class ElectionTests {
    public static void main(String[] args) {

        System.out.println("Running automated tests.");
        testWithImmediateResponses();
    }

    private static void testWithImmediateResponses() {
        String serverOutputFileName = "testoutputs/server_1.txt";
        String electionManagerOutputFileName = "testoutputs/election_1.txt";

        PaxosServer server = new PaxosServer();
        Thread serverThread = new Thread(() -> {
            String[] args = { serverOutputFileName };
            server.main(args);
        });
        
        serverThread.start();

        String[] args = {"true", electionManagerOutputFileName};
        ElectionManager.main(args);

        server.closeServer();

        try {
            serverThread.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupted exception for server thread join");
            e.printStackTrace();
        }
    }

    private static void testWithDelayedAndAbsentResponses() {
        
    }
}
