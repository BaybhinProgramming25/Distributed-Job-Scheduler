package orchestrator;

import java.util.HashSet;

public class Orchestrator {

    private static final Orchestrator orchestrator; 


    static {
       HashSet<String> connections = new HashSet<String>();
    
    }

    public static Orchestrator getOrchestrator() {
        return orchestrator; 
    }
    
}