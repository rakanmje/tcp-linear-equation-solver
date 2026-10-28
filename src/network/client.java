package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
 
// Client application: connects to the server, submits equations, and displays results with RTT
public class client {
 
    private static final String REMOTE_HOST = "127.0.0.1"; // server address (loopback for local testing)
    private static final int    REMOTE_PORT = 5000;         // must match the server's binding port
 
    public static void main(String[] args) throws IOException {
        runClientSession(); // delegate full session lifecycle to helper method
    }
 
    // Initializes socket and streams, runs the menu loop, then releases all resources
    private static void runClientSession() throws IOException {
        Socket         remoteServerSocket  = new Socket(REMOTE_HOST, REMOTE_PORT); // establish TCP connection
        BufferedReader networkInputStream  = new BufferedReader(
                new InputStreamReader(remoteServerSocket.getInputStream())); // wrap socket input stream
        PrintWriter    networkOutputStream = new PrintWriter(remoteServerSocket.getOutputStream(), true); // wrap output stream with auto-flush
        Scanner        consoleScanner      = new Scanner(System.in); // scanner for console input
 
        System.out.println("[REMOTE] " + networkInputStream.readLine()); // receive and display server prompt
        executeMenuLoop(networkInputStream, networkOutputStream, consoleScanner);
 
        remoteServerSocket.close(); // release socket on exit
        consoleScanner.close();
    }
 
    // Presents menu options repeatedly until the user selects exit
    private static void executeMenuLoop(BufferedReader networkInputStream,
                                        PrintWriter    networkOutputStream,
                                        Scanner        consoleScanner) throws IOException {
        boolean sessionActive = true;
        do {
            System.out.println("\n========== MENU ==========");
            System.out.println(" [1] Submit a new equation");
            System.out.println(" [2] Disconnect and exit");
            System.out.print("Selection: ");
 
            String menuSelection = consoleScanner.next(); // read user selection
 
            if (menuSelection.equals("2")) {
                System.out.println("[CLIENT] Terminating session. Goodbye.");
                sessionActive = false; // exit condition for the loop
 
            } else if (menuSelection.equals("1")) {
                handleEquationSubmission(networkInputStream, networkOutputStream, consoleScanner); // process submission
 
            } else {
                System.out.println("[CLIENT] Unrecognised option — please enter 1 or 2."); // handle invalid input
            }
 
        } while (sessionActive);
    }
 
    // Collects four integer parameters, transmits the equation, and prints the server response with RTT
    private static void handleEquationSubmission(BufferedReader networkInputStream,
                                                 PrintWriter    networkOutputStream,
                                                 Scanner        consoleScanner) throws IOException {
        int firstCoefficient  = promptForInteger(consoleScanner, "Coefficient P1"); // read A
        int secondCoefficient = promptForInteger(consoleScanner, "Coefficient P2"); // read B
        int thirdCoefficient  = promptForInteger(consoleScanner, "Coefficient P3"); // read C
        int rhsTarget         = promptForInteger(consoleScanner, "Target value (RHS)"); // read target
 
        String transmissionPayload = firstCoefficient + " " + secondCoefficient + " "
                                   + thirdCoefficient + " " + rhsTarget; // compose space-delimited payload
 
        long dispatchTimestamp  = System.nanoTime(); // timestamp before transmission
        networkOutputStream.println(transmissionPayload); // send equation to server
        String serverReply      = networkInputStream.readLine(); // await server response
        long receptionTimestamp = System.nanoTime(); // timestamp after reception
 
        long elapsedMilliseconds = (receptionTimestamp - dispatchTimestamp) / 1_000_000L; // compute RTT in ms
 
        System.out.println("  Equation : " + renderEquationExpression(transmissionPayload));
        System.out.println("  Result   : " + serverReply);
        System.out.println("  RTT      : " + elapsedMilliseconds + " ms");
    }
 
    // Validates and returns an integer from user input; re-prompts on NumberFormatException
    private static int promptForInteger(Scanner consoleScanner, String fieldLabel) {
        do {
            System.out.print("  Enter " + fieldLabel + " (integer): ");
            String rawEntry = consoleScanner.next();
            try {
                return Integer.parseInt(rawEntry); // return parsed value on success
            } catch (NumberFormatException parseEx) {
                System.out.println("  [!] Invalid input — " + fieldLabel + " must be a whole number."); // re-prompt on failure
            }
        } while (true);
    }
 
    // Formats a raw "P1 P2 P3 R" payload into a readable algebraic expression
    private static String renderEquationExpression(String rawPayload) {
        String[] tokenArray = rawPayload.split(" ");
        if (tokenArray.length == 4) { // guard: only format if all four tokens are present
            return tokenArray[0] + "*x + " + tokenArray[1] + "*y + "
                 + tokenArray[2] + "*z = " + tokenArray[3];
        }
        return rawPayload; // return original string if token count is unexpected
    }
}
 
