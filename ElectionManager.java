import java.util.ArrayList;
import java.util.List;

public class ElectionManager {
    public static ArrayList<Member> members = new ArrayList<>();
    public static int existingElectionWinner = -1;
    public static List<Thread> memberThreads = new ArrayList<>();
    public static volatile boolean electionFinished = false;

    
    public static void main(String[] args) {
        initMembers();
        
        runElection();
    }
    
    //MAJORITY CONSTANTS (2)
    public static void initMembers() {
        for (int memberId = 1; memberId < 10; memberId++) {
            members.add(new Member(memberId, memberId < 4));
        }
    }
    
    public static void runElection() {
        
        for (Member member : members) {
            Thread memberThread = new Thread(() -> {
                String winner = member.elect();
                if (winner != null) {
                    int winnerInt = Integer.parseInt(winner);
                    if (existingElectionWinner == -1 || winnerInt == existingElectionWinner) {
                        existingElectionWinner = winnerInt;
                        electionFinished = true;
                    }
                }
            });
            
            memberThread.setName("member" + member.memberId);
            
            memberThreads.add(memberThread);
            
            memberThread.start();
        }

        while (!electionFinished) {}

        finishElection();
        
        System.out.println("\nMember " + existingElectionWinner + " won the election!");
    }
    
    public static void finishElection() {
        System.out.println("Finishing");
        for (Member member : members) {
            member.finishElection();
        }
        
        for (Thread thread : memberThreads) {
            try {
                thread.join();
                System.out.println("Thread " + thread.getName() + " joined");
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception for thread join: " + thread.getName());
                e.printStackTrace();
                System.out.println("Exiting.");
            }
        }
        
    }
}
