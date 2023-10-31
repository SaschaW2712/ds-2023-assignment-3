import java.util.ArrayList;
import java.util.List;

public class ElectionManager {
    public static ArrayList<Member> members = new ArrayList<>();
    public static int existingElectionWinner = -1;

    public static void main(String[] args) {
        initMembers();

        runElection();
    }

    //MAJORITY CONSTANTS (2)
    public static void initMembers() {
        for (int memberId = 1; memberId < 6; memberId++) {
            members.add(new Member(memberId, memberId < 3));
        }
    }

    public static void runElection() {
        List<Thread> memberThreads = new ArrayList<>();

        for (Member member : members) {
            Thread memberThread = new Thread(() -> {
                String winner = member.elect();
                if (winner != null) {
                    int winnerInt = Integer.parseInt(winner);
                    if (existingElectionWinner == -1 || winnerInt == existingElectionWinner) {
                        existingElectionWinner = winnerInt;
                    }
                }
            });

            memberThread.setName("member" + member.memberId);

            memberThreads.add(memberThread);
            
            memberThread.start();
        }


        for (Thread thread : memberThreads) {
            try {
                thread.join();
                thread.interrupt();
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception for thread join: " + thread.getName());
                e.printStackTrace();
                System.out.println("Exiting.");
            }
        }

        System.out.println("Member " + existingElectionWinner + " won the election!");
    }
}
