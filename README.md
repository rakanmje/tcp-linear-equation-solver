# TCP Linear Equation Solver

A multi-threaded **client–server application in Java** that solves linear equations of the form

```
P1·x + P2·y + P3·z = Target
```

over a TCP socket connection. The client sends the coefficients, the server searches for a non-negative integer solution and sends it back, and the client reports the result together with the measured **Round-Trip Time (RTT)**.

> University course project for **CS1330 — Computer Networks** at Imam Mohammad Ibn Saud Islamic University.

---

## Features

- **TCP socket communication** using `ServerSocket` and `Socket` with line-based text streams.
- **Concurrent clients**: every accepted connection is handed to its own `SessionWorker` thread, so several clients can be served at once.
- **Persistent sessions**: one TCP connection per client, with any number of equations submitted through a simple menu.
- **RTT measurement** with `System.nanoTime()` for each request.
- **Input validation** on the client: non-integer input is rejected and re-prompted before anything is sent.

---

## Project Structure

```
tcp-linear-equation-solver/
├── src/
│   └── network/
│       ├── server.java     # Server entry point + SessionWorker thread class
│       └── client.java     # Interactive client with menu and RTT measurement
├── .gitignore
└── README.md
```

---

## How It Works

```
 Client                                   Server (port 5000)
   │  ── TCP connect ───────────────────▶   │  accept() → new SessionWorker thread
   │  ◀── prompt message ────────────────   │
   │  ── "P1 P2 P3 Target" ─────────────▶   │  exhaustive search over x, y, z ∈ [0, 100]
   │  ◀── "Solution: x=.., y=.., z=.."  ──  │
   │       or "No Answer"                   │
   │  (RTT = receive time − send time)      │
```

**Server (`server.java`)**

- Binds a `ServerSocket` to port **5000** and loops on `accept()`.
- Each client socket is passed to a `SessionWorker` (`Runnable`) running in a new thread.
- The worker reads one line per request, parses the four integers, and calls `computeIntegerSolution()`.
- The solver tries every combination of x, y, z from 0 to 100 (an O(n³) search) and returns the first one that satisfies the equation, or `No Answer` if none exists in that range.

**Client (`client.java`)**

- Connects to the server (`REMOTE_HOST`, port 5000) and displays the server's prompt.
- Menu: `[1]` submit a new equation, `[2]` disconnect and exit.
- Reads the four integers with validation, sends them as one space-separated line, and times the round trip.
- Prints the equation, the server's result, and the RTT in milliseconds.

---

## Requirements

- **JDK 8 or later**
- Two machines on the same LAN for networked testing, or a single machine using `127.0.0.1`

---

## How to Run

### 1. Compile

From the repository root:

```bash
javac -d out src/network/*.java
```

### 2. Start the server

```bash
java -cp out network.server
```

```
[SERVER] System online and awaiting requests on port 5000...
```

### 3. Start the client (in another terminal or on another machine)

```bash
java -cp out network.client
```

### Running on two machines

1. Find the server's LAN IP address (`ip a` on Linux/macOS, `ipconfig` on Windows).
2. In `client.java`, set `REMOTE_HOST` to that address instead of `127.0.0.1`, then recompile.
3. Allow inbound TCP traffic on port **5000** in the server machine's firewall.
4. Start the server first, then the client.

In Eclipse: create a Java project, add a package named `network`, place both files in it, and run `server` before `client`.

---

## Example Session

```
========== MENU ==========
 [1] Submit a new equation
 [2] Disconnect and exit
Selection: 1
  Enter Coefficient P1 (integer): 1
  Enter Coefficient P2 (integer): 2
  Enter Coefficient P3 (integer): 3
  Enter Target value (RHS) (integer): 48
  Equation : 1*x + 2*y + 3*z = 48
  Result   : Solution: x=0, y=0, z=16
  RTT      : 21 ms
```

---

## RTT Observations

RTT depends mainly on how far the server's search has to go. Equations solved with small values of x and y return almost immediately, while equations with no solution force the server through all 101³ ≈ 1.03 million combinations before answering, which gives the longest RTTs.

| Equation | Result | RTT (ms) |
|---|---|---:|
| 1x + 1y + 1z = 1 | x=0, y=0, z=1 | 1 |
| 1x + 2y + 3z = 48 | x=0, y=0, z=16 | 21 |
| 5x + 7y + 3z = 500 | x=0, y=29, z=99 | 1 |
| 0x + 0y + 0z = 100 | No Answer | 16 |
| 7x + 11y + 13z = 1 | No Answer | 23 |

Measurements were taken on a virtual LAN (Arch Linux host as server, a virtual machine as client); exact values vary between runs, and the first request of a session is usually slower because of JVM warm-up.

---

## Limitations

- Only **non-negative integer** solutions in the range 0–100 are searched, so a "No Answer" result means no solution exists within that range, not necessarily none at all.
- The server trusts that each line contains four valid integers. The bundled client guarantees this, but a different client sending malformed data would end that session with an exception.
- Server address and port are hard-coded constants rather than command-line arguments.

---

## Team

- **Rakan Fahad Al-Shammari**
- **Rakan Majed Alharbi**
- **Ibrahim Abdulaziz Alrumaih**
