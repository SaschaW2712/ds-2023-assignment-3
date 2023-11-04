import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import dataclasses.Location;
import dataclasses.LocationWithRegularity;
import dataclasses.MemberResponsiveness;
import enums.InternetSpeed;
import enums.Regularity;
import enums.ResponseLikelihood;

public class Member {
    public PrintStream outputStream = new PrintStream(System.out);

    int memberId;
    Proposer proposer;
    Acceptor acceptor;
    String electionWinnerMemberId;
    MemberResponsiveness responsiveness;
    
    boolean immediateResponse = false;
    
    Thread proposerThread;
    Thread acceptorThread;

    int port = 4567;
    
    public Member(int memberId, boolean shouldPropose) {
        this.memberId = memberId;
        
        if (shouldPropose) {
            //MAJORITY CONSTANT
            this.proposer = new Proposer(memberId, 9);
        }
        
        initMemberResponsiveness(memberId);
        this.acceptor = new Acceptor(memberId, responsiveness);
    }
    
    public Member(int memberId, boolean shouldPropose, boolean immediateResponse, PrintStream outputStream, int port) {
        this.memberId = memberId;

        if (outputStream != null) {
            this.outputStream = outputStream;
        }
        
        this.port = port;
        
        if (shouldPropose) {
            //MAJORITY CONSTANT
            this.proposer = new Proposer(memberId, 9, outputStream, port);
        }
        
        this.immediateResponse = immediateResponse;
        
        initMemberResponsiveness(memberId);
        this.acceptor = new Acceptor(memberId, responsiveness, immediateResponse, outputStream, port);
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
                return electionWinnerMemberId;
            } catch (InterruptedException e) {
                outputStream.println("Interrupted exception for thread join: " + proposerThread.getName());
                e.printStackTrace();
                return null;
            }
        }
        
        //Non-proposers won't return a voting result
        return null;
    }
    
    
    public void runProposal(Proposer proposer) {
        String result = proposer.propose(Integer.toString(proposer.memberId));
        
        
        if (result.startsWith("SUCCESS")) {
            electionWinnerMemberId = result.split("\\s+")[1];
            outputStream.println("Member ID " + memberId + " succeeded on proposal with value " + electionWinnerMemberId);
        } else if (result.startsWith("ENDED")) {
            return;
        } else {
            runProposal(proposer);
        }
    }
    
    public void finishElection() {
        try {
            if (proposer != null && proposerThread.isAlive()) {
                proposer.finish();
                proposerThread.join();
            }
            
            acceptor.closeSocket();
            if (acceptorThread.isAlive()) {
                acceptorThread.join();
            }
            
        } catch (InterruptedException e) {
            outputStream.println("Interrupted exception for thread join: " + proposerThread.getName());
            e.printStackTrace();
        }
        
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
            memberLocations.add(new LocationWithRegularity(workLocation, Regularity.Rarely));
        }
        
        this.responsiveness = new MemberResponsiveness(memberLocations);
    }
}
