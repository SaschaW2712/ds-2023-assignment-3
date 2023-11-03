import java.util.concurrent.TimeUnit;

public class ElectionTests {
    public static void main(String[] args) {

        System.out.println("Running automated tests.");

        Thread serverThread = new Thread(() -> {
            String[] serverArgs = { "testoutputs/server.txt" };
            PaxosServer.main(serverArgs);
        });

        serverThread.start();

        testWithImmediateResponses();

        try {
            TimeUnit.SECONDS.sleep(5); //Give previous test time to resolve and reset
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testWithDelayedAndAbsentResponses();
    }

    private static void testWithImmediateResponses() {
        System.out.println("Running testWithImmediateResponses");
        String electionManagerOutputFileName = "testoutputs/election_1.txt";
        
        String[] args = {"true", electionManagerOutputFileName};
        ElectionManager.main(args);

    }

    private static void testWithDelayedAndAbsentResponses() {
        System.out.println("Running testWithDelayedAndAbsentResponses");

        String electionManagerOutputFileName = "testoutputs/election_2.txt";

        String[] args = { "false", electionManagerOutputFileName};
        ElectionManager.main(args);
    }
}
