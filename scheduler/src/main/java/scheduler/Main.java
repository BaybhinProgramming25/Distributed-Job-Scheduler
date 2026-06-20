package scheduler;

import java.util.HashSet;

import orchestrator.Orchestrator;


public class Main {
    public static void main(String[] args) {
       
        // HashSet that will store all of the client_addresses 
        HashSet<String> client_addresses = new HashSet<String>();

        // Add the client addresses
        client_addresses.add("http://127.0.0.1:60060");
        client_addresses.add("http://127.0.0.1:60061");
        client_addresses.add("http://127.0.0.1:60062");

        



    }
}