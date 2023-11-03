import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ElectionManager {
    
    public static PrintStream outputStream = new PrintStream(System.out);
    
    public static ArrayList<Member> members = new ArrayList<>();
    public static int existingElectionWinner = -1;
    public static List<Thread> memberThreads = new ArrayList<>();
    public static volatile boolean electionFinished = false;
    
    public static boolean immediateResponse = false;
    
    
    public static void main(String[] args) {
        if (args.length > 0) {
            immediateResponse = Boolean.parseBoolean(args[0]);
        }
        
        if (args.length > 1) {
            //Redirect system output if requested
            try {
                PrintWriter writer = new PrintWriter(args[1]);
                writer.print("");
                writer.close();
                
                outputStream = new PrintStream(new FileOutputStream(args[1], true));
            } catch(FileNotFoundException e) {
                outputStream.println("Couldn't find output file");
                return;
            }
        } 
        
        initMembers();
        
        runElection();
    }
    
    //MAJORITY CONSTANTS (2)
    public static void initMembers() {
        for (int memberId = 1; memberId < 10; memberId++) {
            members.add(new Member(memberId, memberId < 4, immediateResponse, outputStream));
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
        
        outputStream.println("\nMember " + existingElectionWinner + " won the election!");
        outputStream.println("You can now exit the Paxos server.");
    }
    
    public static void finishElection() {
        outputStream.println("\nFinishing election and cleaning up threads\n");
        for (Member member : members) {
            member.finishElection();
        }
        
        for (Thread thread : memberThreads) {
            try {
                thread.join();
                outputStream.println("Thread " + thread.getName() + " joined");
            } catch (InterruptedException e) {
                outputStream.println("Interrupted exception for thread join: " + thread.getName());
                e.printStackTrace();
                outputStream.println("Exiting.");
            }
        }
    }
}
