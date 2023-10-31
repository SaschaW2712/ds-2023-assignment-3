import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import dataclasses.Location;
import dataclasses.LocationWithRegularity;
import dataclasses.MemberResponsiveness;
import dataclasses.ResponseWithOptionalProposal;
import enums.InternetSpeed;
import enums.Regularity;
import enums.ResponseLikelihood;

public class Member {
    int memberId;
    Proposer proposer;
    Acceptor acceptor;
    String electionWinnerMemberId;
    MemberResponsiveness responsiveness;
    
    Thread proposerThread;
    Thread acceptorThread;
    
    public Member(int memberId, boolean shouldPropose) {
        this.memberId = memberId;
        
        if (shouldPropose) {
            //MAJORITY CONSTANT
            this.proposer = new Proposer(memberId, 9);
        }
        
        initMemberResponsiveness(memberId);
        this.acceptor = new Acceptor(memberId, responsiveness);
    }
    
    public String elect() {
        if (proposer != null) {
            proposerThread = new Thread(() -> {
                
                runProposal(proposer);
            });
            
            proposerThread.setName("proposer" + memberId);
            proposerThread.start();
        }
        
        acceptorThread = new Thread(() -> {
            acceptor.listenToServer();
        });
        
        acceptorThread.setName("acceptor" + memberId);
        acceptorThread.start();
        
        if (proposerThread != null) {
            try {
                proposerThread.join();
                System.out.println("here");
                return electionWinnerMemberId;
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception for thread join: " + proposerThread.getName());
                e.printStackTrace();
                return null;
            }
        }
        
        //Non-proposers won't return a voting result
        return null;
    }
    
    
    public void runProposal(Proposer proposer) {
        String result = proposer.propose(Integer.toString(proposer.memberId));
        
        System.out.println("Result for memberId " + proposer.memberId + ": " + result);
        
        if (result.startsWith("SUCCESS")) {
            electionWinnerMemberId = result.split("\\s+")[1];
        } else {
            runProposal(proposer);
        }
    }
    
    public String isElectionDone() {
        String result = "";
        
        try {
            Socket socket = new Socket("localhost", 4567);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            out.println("Election Result");
            
            
            String line;
            while ((line = in.readLine()) == null) {}
            
            if (line != null && line.startsWith("Done")) {
                result = line.split("\\s+")[1];
            }
            
            socket.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
    
    public void initMemberResponsiveness(int memberId) {
        List<LocationWithRegularity> memberLocations = new ArrayList<LocationWithRegularity>();
        
        switch (memberId) {
            case 1:
            Location member1HomeLocation = new Location(InternetSpeed.High, ResponseLikelihood.Certain);
            memberLocations.add(new LocationWithRegularity(member1HomeLocation, Regularity.Always));
            break;
            case 2:
            Location hillsLocation = new Location(InternetSpeed.Low, ResponseLikelihood.Improbable);
            Location cafeLocation = new Location(InternetSpeed.High, ResponseLikelihood.Certain);
            memberLocations.add(new LocationWithRegularity(hillsLocation, Regularity.Often));
            memberLocations.add(new LocationWithRegularity(cafeLocation, Regularity.Rarely));
            break;
            case 3:
            Location member3HomeLocation = new Location(InternetSpeed.High, ResponseLikelihood.Certain);
            Location coorongLocation = new Location(InternetSpeed.Low, ResponseLikelihood.Impossible);
            memberLocations.add(new LocationWithRegularity(member3HomeLocation, Regularity.Often));
            memberLocations.add(new LocationWithRegularity(coorongLocation, Regularity.Rarely));
            break;
            default:
            Location otherMembersHomeLocation = new Location(InternetSpeed.High, ResponseLikelihood.Certain);
            Location workLocation = new Location(InternetSpeed.Medium, ResponseLikelihood.Impossible);
            memberLocations.add(new LocationWithRegularity(otherMembersHomeLocation, Regularity.Sometimes));
            memberLocations.add(new LocationWithRegularity(workLocation, Regularity.Sometimes));
        }
        
        this.responsiveness = new MemberResponsiveness(memberLocations);
    }
}
