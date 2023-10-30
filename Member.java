import java.util.concurrent.TimeUnit;

public class Member {
    int memberId;
    Proposer proposer;
    Acceptor acceptor;
    String electionWinner;

    public Member(int memberId, boolean shouldPropose) {
        this.memberId = memberId;
        this.acceptor = new Acceptor(memberId);

        if (shouldPropose) {
            this.proposer = new Proposer(memberId, 9);
        }
    }

    public void elect() {
        Thread proposerThread = new Thread(() -> {
            runProposal(proposer);
        });
        
        proposerThread.start();

        
    }

    
    public void runProposal(Proposer proposer) {
        long delay = 5 - proposer.memberId;

        try {
            TimeUnit.SECONDS.sleep(delay);
        } catch (InterruptedException e) {
                System.out.println("Interrupted exception for start delay");
                e.printStackTrace();
        }

        String result = proposer.propose(Integer.toString(proposer.memberId));

        System.out.println();
        System.out.println("Result for memberId " + proposer.memberId + ": " + result);
        System.out.println();

        if (result.startsWith("SUCCESS")) {
            int winnerId = Integer.parseInt(result.split("\\s+")[1]);
            electionWinner = "M" + (winnerId);
        }
    }
}
