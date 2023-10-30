import java.util.ArrayList;
import java.util.List;

public class ElectionManager {
    public static ArrayList<Member> members;
    public static void main(String[] args) {
        initMembers();

        runElection();
    }

    public static void initMembers() {
        for (int memberId = 1; memberId < 3; memberId++) {
            members.add(new Member(memberId, memberId < 2));
        }
    }

    public static void runElection() {
        List<Thread> memberThreads = new ArrayList<>();

        for (Member member : members) {
            Thread memberThread = new Thread(() -> {
                member.elect();
            });

            memberThread.setName("member" + member.memberId);

            memberThreads.add(memberThread);
            
            memberThread.start();
        }

        for (Thread thread: memberThreads) {
            try {
                thread.join();
                System.out.println("Thread done: " + thread.getName());
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception for thread join: " + thread.getName());
                e.printStackTrace();
                return;
            }
        }
    }
}
