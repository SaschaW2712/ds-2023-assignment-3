import java.util.ArrayList;
import java.util.List;

public class ElectionManager {
    public static ArrayList<Member> members = new ArrayList<>();
    public static void main(String[] args) {
        initMembers();

        runElection();

        cleanupThreads();
    }

    public static void initMembers() {
        for (int memberId = 1; memberId < 10; memberId++) {
            members.add(new Member(memberId, memberId < 4));
        }
    }

    public static void runElection() {
        List<Thread> memberThreads = new ArrayList<>();

        for (Member member : members) {
            Thread memberThread = new Thread(() -> {
                String electionWinner = member.elect();
                if (electionWinner != null) {
                    System.out.println("Member " + electionWinner + " won the election!");
                }
            });

            memberThread.setName("member" + member.memberId);

            memberThreads.add(memberThread);
            
            memberThread.start();
        }


        for (Thread thread : memberThreads) {
            try {
                thread.join(2000);
                thread.interrupt();
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception for thread join: " + thread.getName());
                e.printStackTrace();
                System.out.println("Exiting.");
            }
        }
    }

    public static void cleanupThreads() {
        for (Member member : members) {
            Thread pThread = member.proposerThread;
            Thread aThread = member.acceptorThread;

            if (pThread != null && pThread.isAlive()) {
                pThread.interrupt();
            }

            if (aThread.isAlive()) {
                aThread.interrupt();
            }
        }
    }
}
