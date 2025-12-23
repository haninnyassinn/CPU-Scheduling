import java.util.*;

public class Main {

    // phase names
    static final int FCFS = 0; // First 25% of the quantum - First Come First Serve phase
    static final int PRIORITY = 1; // Second 25% of quantum - Priority-based scheduling phase
    static final int SJF = 2; // Last 50% of quantum - Shortest Job First phase

    // process class representing each process in the system
    public static class Process {
        String name; // process identifier
        int arrival; // arrival time when process enters the system
        int burst; // total CPU time needed (burst time)
        int remaining; // remaining execution time (decrements as process runs)
        int priority; // priority level (smaller number = higher priority)
        int quantum; // current quantum time allocated to this process
        int usedInQuantum; // how much quantum has been used in current quantum cycle
        int turnaround; // turnaround time (finish time - arrival time)
        List<Integer> quantumHistory; // history of quantum values as they change over time

        // Constructor to initialize a process
        Process(String name, int arrival, int burst, int priority, int quantum) {
            this.name = name;
            this.arrival = arrival;
            this.burst = burst;
            this.remaining = burst; // initially remaining equals total burst
            this.priority = priority;
            this.quantum = quantum;
            this.usedInQuantum = 0; // no quantum used initially
            this.turnaround = 0; // will be calculated when process finishes
            quantumHistory = new ArrayList<>();
            quantumHistory.add(quantum); // save initial quantum value
        }

        // determines which scheduling phase the process is currently in based on quantum usage
        int getPhase() {
            // Calculate 25% and 50% thresholds of quantum
            int first25 = (int) Math.ceil(quantum * 0.25); // first 25%
            int first50 = first25 * 2; // second 25%

            // phase choosed based on how much quantum has been used
            if (usedInQuantum < first25) return FCFS; // first 25%: FCFS phase
            if (usedInQuantum < first50) return PRIORITY; // second 25%: Priority phase
            return SJF; // last 50%: SJF phase
        }
    }

    // stores final results for a single process
    public static class ProcessResult {
        String name; // process name
        int waitingTime; // total waiting time (turnaround - burst)
        int turnaroundTime; //total turnaround time
        List<Integer> quantumHistory; // history of quantum values

        ProcessResult(String n, int w, int t, List<Integer> q) {
            name = n;
            waitingTime = w;
            turnaroundTime = t;
            quantumHistory = q;
        }
    }

    // complete result of the scheduler run
    public static class Result {
        List<String> order; // execution order of processes (which process ran when)
        List<ProcessResult> processResults; // detailed results for each process
        double avgW; // average waiting time across all processes
        double avgT; // average turnaround time across all processes

        Result(List<String> o, List<ProcessResult> p, double w, double t) {
            order = o;
            processResults = p;
            avgW = w;
            avgT = t;
        }
    }

    //run function
    public static Result run(List<Process> processes) {
        int time = 0; // current simulation time (CPU clock)
        int completed = 0; // number of processes that have finished execution
        int n = processes.size(); //total number of processes
        Process current = null; //currently running process (null if CPU idle)
        List<Process> ready = new ArrayList<>(); //ready queue (processes waiting for CPU)
        List<String> executionOrder = new ArrayList<>(); //timeline of which process ran

        // Track previous phase to detect phase changes
        int previousPhase = -1; // no previous phase initially

        // add flag to track if we already checked for preemption in PRIORITY phase
        boolean priorityPreemptionChecked = false; // prevents multiple priority checks in same phase

        // initialize ready queue with processes that arrive at time 0
        for (int i = 0; i < n; i++) {
            if (processes.get(i).arrival == time) {
                ready.add(processes.get(i));
            }
        }

        // main scheduling loop - runs until all processes complete
        while (completed < n) {

            // If CPU is idle, try to schedule a new process
            if (current == null) {
                // If ready queue is empty
                if (ready.size() == 0) {
                    time++; // add time by 1 unit

                    // add any processes that arrive at this new time
                    for (int i = 0; i < n; i++) {
                        if (processes.get(i).arrival == time && processes.get(i).remaining > 0) {
                            ready.add(processes.get(i));
                        }
                    }
                    continue; // go back to while loop start
                }

                // pick first one (FCFS)
                current = ready.remove(0); // FCFS selection from ready queue
                // update execution order (only if different from last process)
                if (executionOrder.size() == 0) { // first process to run
                    executionOrder.add(current.name);
                } else { // Check if different from previous process
                    String last = executionOrder.get(executionOrder.size() - 1);
                    if (!last.equals(current.name)) {
                        executionOrder.add(current.name);
                    }
                }

                // reset flags for new process
                priorityPreemptionChecked = false;
                previousPhase = -1;
            }

            // execute current process for 1 time unit
            current.remaining--; // decrement remaining execution time
            current.usedInQuantum++; // increment quantum usage
            time++; // add time

            // check if current process just finished
            if (current.remaining == 0) {
                current.turnaround = time - current.arrival; // calculate turnaround time
                current.quantumHistory.add(0); // add 0 to quantum history (process finished)
                completed++; // increment completed count
                current = null; // cPU now idle
                priorityPreemptionChecked = false; // reset preemption flag

                // add any processes that arrived exactly at this finish time
                for (int i = 0; i < n; i++) {
                    Process p = processes.get(i);
                    if (p.arrival == time && p.remaining > 0 && !ready.contains(p)) {
                        ready.add(p);
                    }
                }
                continue; // go back to while loop start
            }

            // check if process used all its quantum but still has work left
            if (current.usedInQuantum == current.quantum) {
                current.quantum += 2; // increase quantum by 2 (Adaptive Garaging)
                current.quantumHistory.add(current.quantum); // record new quantum
                current.usedInQuantum = 0; // reset quantum usage
                ready.add(current); // put process back in ready queue
                current = null; // CPU now idle
                priorityPreemptionChecked = false; // reset preemption flag
                continue; // back to while loop start
            }

            // add newly arrived processes to ready queue
            for (int i = 0; i < n; i++) {
                if (processes.get(i).arrival == time && !ready.contains(processes.get(i))
                        && processes.get(i) != current && processes.get(i).remaining !=0) {
                    ready.add(processes.get(i));
                }
            }

            // get current phase and detect phase change
            int currentPhase = current.getPhase();
            boolean phaseChanged = (previousPhase != currentPhase);
            previousPhase = currentPhase; // update previous phase

            //PRIORITY PHASE
            // Preemption only when ENTERING priority phase
            if (currentPhase == PRIORITY && (phaseChanged || !priorityPreemptionChecked)) {
                // reset the flag since we're checking now
                priorityPreemptionChecked = true;

                // find process with highest priority (smallest number) among ready and current
                Process best = current;
                for (int i = 0; i < ready.size(); i++) {
                    Process p = ready.get(i);
                    if (p.priority < best.priority) {
                        best = p;
                    }
                }

                // If a higher priority process exists in ready queue
                if (best != current) {
                    // calculate remaining quantum for current process
                    int remainingQ = current.quantum - current.usedInQuantum;
                    // add half of remaining quantum to current process's quantum (penalty)
                    int addedQ = (int) Math.ceil(remainingQ / 2.0);
                    current.quantum += addedQ; // increase quantum
                    current.quantumHistory.add(current.quantum); // record quantum change
                    current.usedInQuantum = 0; // reset quantum usage
                    ready.add(current); // put preempted process back in ready queue
                    ready.remove(best); // remove new process from ready queue
                    current = best; // switch to higher priority process
                    priorityPreemptionChecked = false; // reset for new process

                    executionOrder.add(current.name); // update execution order
                }
            }

            // SJF PHASE
            // check every time unit for shorter processes
            if (currentPhase == SJF) {
                // find process with shortest remaining time among ready and current
                Process shortest = current;
                for (int i = 0; i < ready.size(); i++) {
                    Process p = ready.get(i);
                    if (p.remaining < shortest.remaining) {
                        shortest = p;
                    }
                }

                // If a shorter process exists in ready queue
                if (shortest != current) {
                    // give all remaining quantum as penalty to current process
                    int remainingQ = current.quantum - current.usedInQuantum;
                    current.quantum += remainingQ; // add remaining quantum
                    current.quantumHistory.add(current.quantum); // record quantum change
                    current.usedInQuantum = 0; // reset quantum usage
                    ready.add(current); // put preempted process back in ready queue
                    ready.remove(shortest); // remove new process from ready queue
                    current = shortest; // switch to shorter process
                    executionOrder.add(current.name); // update execution order
                }
            }
        }

        // calculate final results for all processes
        List<ProcessResult> results = new ArrayList<>();
        double totalW = 0; // total waiting time
        double totalT = 0; // total turnaround time

        for (int i = 0; i < n; i++) {
            Process p = processes.get(i);
            int waiting = p.turnaround - p.burst; // Waiting time = turnaround - burst
            results.add(new ProcessResult(
                    p.name,
                    waiting,
                    p.turnaround,
                    p.quantumHistory
            ));
            totalW += waiting;
            totalT += p.turnaround;
        }

        // return final results
        return new Result(
                executionOrder,
                results,
                totalW / n, // Average waiting time
                totalT / n  // Average turnaround time
        );
    }

    public static void main(String[] args) {

        List<Process> processes1 = new ArrayList<>();
        System.out.println("Test Case 5");
        processes1.add(new Process("P1", 0, 25, 3, 5));
        processes1.add(new Process("P2", 1, 18, 2, 4));
        processes1.add(new Process("P3", 3, 22, 4, 6));
        processes1.add(new Process("P4", 5, 15, 1, 3));
        processes1.add(new Process("P5", 8, 20, 5, 7));
        processes1.add(new Process("P6", 12, 12, 6, 4));

        Result result1 = run(processes1);
        System.out.println("Execution Order: " + result1.order);
        for (ProcessResult pr : result1.processResults) {
            System.out.println("Process " + pr.name +
                    ": Waiting Time = " + pr.waitingTime +
                    ", Turnaround Time = " + pr.turnaroundTime +
                    ", Quantum History = " + pr.quantumHistory);
        }
        System.out.println("Average Waiting Time: " + result1.avgW);
        System.out.println("Average Turnaround Time: " + result1.avgT);

        System.out.println("\n---------------------------------\n");


        List<Process> processes2 = new ArrayList<>();
        System.out.println("Test Case 6");
        processes2.add(new Process("P1", 0, 14, 4, 6));
        processes2.add(new Process("P2", 4, 9, 2, 8));
        processes2.add(new Process("P3", 7, 16, 5, 5));
        processes2.add(new Process("P4", 10, 7, 1, 10));
        processes2.add(new Process("P5", 15, 11, 3, 4));
        processes2.add(new Process("P6", 20, 5, 6, 7));
        processes2.add(new Process("P7", 25, 8, 7, 9));

        Result result2 = run(processes2);
        System.out.println("Execution Order: " + result2.order);
        for (ProcessResult pr : result2.processResults) {
            System.out.println("Process " + pr.name +
                    ": Waiting Time = " + pr.waitingTime +
                    ", Turnaround Time = " + pr.turnaroundTime +
                    ", Quantum History = " + pr.quantumHistory);
        }
        System.out.println("Average Waiting Time: " + result2.avgW);
        System.out.println("Average Turnaround Time: " + result2.avgT);
    }

}
