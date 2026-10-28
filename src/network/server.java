package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
 
// Entry point: binds the server socket and dispatches each client to a worker thread
public class server {
 
    private static final int BINDING_PORT = 5000; // designated TCP port for incoming connections
 
    public static void main(String[] args) throws IOException {
        ServerSocket listenerSocket = new ServerSocket(BINDING_PORT); // bind server to port
        System.out.println("[SERVER] System online and awaiting requests on port " + BINDING_PORT + "...");
        acceptClientConnections(listenerSocket);
    }
 
    // Perpetual acceptance loop; spawns a SessionWorker thread per connected client
    private static void acceptClientConnections(ServerSocket listenerSocket) throws IOException {
        do {
            Socket sessionSocket = listenerSocket.accept(); // block until client connects
            System.out.println("[SERVER] Incoming session established — remote host: "
                    + sessionSocket.getInetAddress());
            new Thread(new SessionWorker(sessionSocket)).start(); // delegate to concurrent worker
        } while (true);
    }
}
 
// Runnable handler managing stream I/O and equation processing for one client session
class SessionWorker implements Runnable {
 
    private static final int MAX_SEARCH_BOUND = 100; // upper bound for exhaustive variable search
    private final Socket activeSession; // socket representing the connected client
 
    public SessionWorker(Socket activeSession) {
        this.activeSession = activeSession; // store socket reference for use in run()
    }
 
    @Override
    public void run() {
        try {
            BufferedReader inboundStream  = new BufferedReader(
                    new InputStreamReader(activeSession.getInputStream())); // wrap input stream
            PrintWriter    outboundStream = new PrintWriter(activeSession.getOutputStream(), true); // wrap output stream with auto-flush
 
            outboundStream.println("Awaiting input — provide four integers: [P1] [P2] [P3] [Target]"); // send protocol prompt
            processClientRequests(inboundStream, outboundStream);
            activeSession.close(); // release socket on session end
 
        } catch (IOException ioEx) {
            System.out.println("[SERVER] Session terminated unexpectedly due to an I/O fault."); // log stream failure
        }
    }
 
    // Reads each request line, parses coefficients, and returns the computed solution
    private void processClientRequests(BufferedReader inboundStream,
                                       PrintWriter    outboundStream) throws IOException {
        String incomingDataString;
        while ((incomingDataString = inboundStream.readLine()) != null) { // loop until client disconnects
            String[] rawTokens = incomingDataString.trim().split("\\s+"); // tokenise received input
 
            int P1   = Integer.parseInt(rawTokens[0]); // parse coefficient P1
            int P2    = Integer.parseInt(rawTokens[1]); // parse coefficient P2
            int P3   = Integer.parseInt(rawTokens[2]); // parse coefficient P3
            int targetValue = Integer.parseInt(rawTokens[3]); // parse target (RHS)
 
            String resultPayload = computeIntegerSolution(P1, P2, P3, targetValue); // solve equation
            outboundStream.println(resultPayload); // transmit result to client
        }
    }
 
    // Exhaustive search over [0, MAX_SEARCH_BOUND]^3; returns first triple satisfying P1*x + P2*y + P3*z = target
    private String computeIntegerSolution(int P1, int P2,
                                          int P3, int targetValue) {
        for (int xVal = 0; xVal <= MAX_SEARCH_BOUND; xVal++) {
            for (int yVal = 0; yVal <= MAX_SEARCH_BOUND; yVal++) {
                for (int zVal = 0; zVal <= MAX_SEARCH_BOUND; zVal++) { // iterate all variable combinations
                    int evaluatedSum = P1 * xVal + P2 * yVal + P3 * zVal;
                    if (evaluatedSum == targetValue) // return on first valid solution
                        return "Solution: x=" + xVal + ", y=" + yVal + ", z=" + zVal;
                }
            }
        }
        return "No Answer"; // no solution exists within search bounds
    }
}
 
