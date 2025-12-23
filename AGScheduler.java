import java.util.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
public class AGScheduler {

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

        // Test Case 1
        List<Process> processes1 = new ArrayList<>();
        System.out.println("Test Case 1");
        processes1.add(new Process("P1", 0, 17, 4, 7));
        processes1.add(new Process("P2", 2, 6, 7, 9));
        processes1.add(new Process("P3", 5, 11, 3, 4));
        processes1.add(new Process("P4", 15, 4, 6, 6));
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

        // Test Case 2
        List<Process> processes2 = new ArrayList<>();
        System.out.println("Test Case 2");
        processes2.add(new Process("P1", 0, 10, 3, 4));
        processes2.add(new Process("P2", 0, 8, 1, 5));
        processes2.add(new Process("P3", 0, 12, 2, 6));
        processes2.add(new Process("P4", 0, 6, 4, 3));
        processes2.add(new Process("P5", 0, 9, 5, 4));
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

        System.out.println("\n---------------------------------\n");

        // Test Case 3
        List<Process> processes3 = new ArrayList<>();
        System.out.println("Test Case 3");
        processes3.add(new Process("P1", 0, 20, 5, 8));
        processes3.add(new Process("P2", 3, 4, 3, 6));
        processes3.add(new Process("P3", 6, 3, 4, 5));
        processes3.add(new Process("P4", 10, 2, 2, 4));
        processes3.add(new Process("P5", 15, 5, 6, 7));
        processes3.add(new Process("P6", 20, 6, 1, 3));
        Result result3 = run(processes3);
        System.out.println("Execution Order: " + result3.order);
        for (ProcessResult pr : result3.processResults) {
            System.out.println("Process " + pr.name +
                    ": Waiting Time = " + pr.waitingTime +
                    ", Turnaround Time = " + pr.turnaroundTime +
                    ", Quantum History = " + pr.quantumHistory);
        }
        System.out.println("Average Waiting Time: " + result3.avgW);
        System.out.println("Average Turnaround Time: " + result3.avgT);

        System.out.println("\n---------------------------------\n");

        // Test Case 4
        List<Process> processes4 = new ArrayList<>();
        System.out.println("Test Case 4");
        processes4.add(new Process("P1", 0, 3, 2, 10));
        processes4.add(new Process("P2", 2, 4, 3, 12));
        processes4.add(new Process("P3", 5, 2, 1, 8));
        processes4.add(new Process("P4", 8, 5, 4, 15));
        processes4.add(new Process("P5", 12, 3, 5, 9));
        Result result4 = run(processes4);
        System.out.println("Execution Order: " + result4.order);
        for (ProcessResult pr : result4.processResults) {
            System.out.println("Process " + pr.name +
                    ": Waiting Time = " + pr.waitingTime +
                    ", Turnaround Time = " + pr.turnaroundTime +
                    ", Quantum History = " + pr.quantumHistory);
        }
        System.out.println("Average Waiting Time: " + result4.avgW);
        System.out.println("Average Turnaround Time: " + result4.avgT);

        System.out.println("\n---------------------------------\n");

        //  Test Case 5
        List<Process> processes5 = new ArrayList<>();
        System.out.println("Test Case 5");
        processes5.add(new Process("P1", 0, 25, 3, 5));
        processes5.add(new Process("P2", 1, 18, 2, 4));
        processes5.add(new Process("P3", 3, 22, 4, 6));
        processes5.add(new Process("P4", 5, 15, 1, 3));
        processes5.add(new Process("P5", 8, 20, 5, 7));
        processes5.add(new Process("P6", 12, 12, 6, 4));
        Result result5 = run(processes5);
        System.out.println("Execution Order: " + result5.order);
        for (ProcessResult pr : result5.processResults) {
            System.out.println("Process " + pr.name +
                    ": Waiting Time = " + pr.waitingTime +
                    ", Turnaround Time = " + pr.turnaroundTime +
                    ", Quantum History = " + pr.quantumHistory);
        }
        System.out.println("Average Waiting Time: " + result5.avgW);
        System.out.println("Average Turnaround Time: " + result5.avgT);

        System.out.println("\n---------------------------------\n");

        //  Test Case 6
        List<Process> processes6 = new ArrayList<>();
        System.out.println("Test Case 6");
        processes6.add(new Process("P1", 0, 14, 4, 6));
        processes6.add(new Process("P2", 4, 9, 2, 8));
        processes6.add(new Process("P3", 7, 16, 5, 5));
        processes6.add(new Process("P4", 10, 7, 1, 10));
        processes6.add(new Process("P5", 15, 11, 3, 4));
        processes6.add(new Process("P6", 20, 5, 6, 7));
        processes6.add(new Process("P7", 25, 8, 7, 9));
        Result result6 = run(processes6);
        System.out.println("Execution Order: " + result6.order);
        for (ProcessResult pr : result6.processResults) {
            System.out.println("Process " + pr.name +
                    ": Waiting Time = " + pr.waitingTime +
                    ", Turnaround Time = " + pr.turnaroundTime +
                    ", Quantum History = " + pr.quantumHistory);
        }
        System.out.println("Average Waiting Time: " + result6.avgW);
        System.out.println("Average Turnaround Time: " + result6.avgT);
    }
    //Running tests in unit testing
    private void runTest(String path) throws Exception {

        // Parse JSON
        JsonObject json = JsonParser.parseReader(new FileReader(path)).getAsJsonObject();

        JsonArray inputProcesses = json.getAsJsonObject("input")
                .getAsJsonArray("processes");
        //convert jason into process object
        List<AGScheduler.Process> processes = new ArrayList<>();

        for (JsonElement e : inputProcesses) {
            JsonObject p = e.getAsJsonObject();
            processes.add(new AGScheduler.Process(
                    p.get("name").getAsString(),
                    p.get("arrival").getAsInt(),
                    p.get("burst").getAsInt(),
                    p.get("priority").getAsInt(),
                    p.get("quantum").getAsInt()
            ));
        }

        // run scheduler
        AGScheduler.Result result = AGScheduler.run(processes);

        // expected output
        JsonObject expected = json.getAsJsonObject("expectedOutput");


        // Execution Order

        JsonArray expectedOrder = expected.getAsJsonArray("executionOrder");
        assertEquals(expectedOrder.size(), result.order.size(),
                "Execution order size mismatch");

        for (int i = 0; i < expectedOrder.size(); i++) {
            assertEquals(
                    expectedOrder.get(i).getAsString(),
                    result.order.get(i),
                    "Execution order mismatch at index " + i
            );
        }

        //result for process
        JsonArray expectedProcesses = expected.getAsJsonArray("processResults");
        assertEquals(expectedProcesses.size(), result.processResults.size(),
                "Process results size mismatch");

        for (int i = 0; i < result.processResults.size(); i++) {

            AGScheduler.ProcessResult actual = result.processResults.get(i);
            JsonObject expectedProc = expectedProcesses.get(i).getAsJsonObject();

            // waiting Time
            assertEquals(
                    expectedProc.get("waitingTime").getAsInt(),
                    actual.waitingTime,
                    "Waiting time mismatch for process " + actual.name
            );

            // turnaround Time
            assertEquals(
                    expectedProc.get("turnaroundTime").getAsInt(),
                    actual.turnaroundTime,
                    "Turnaround time mismatch for process " + actual.name
            );

            // quantum History
            JsonArray expectedQH = expectedProc.getAsJsonArray("quantumHistory");
            assertEquals(
                    expectedQH.size(),
                    actual.quantumHistory.size(),
                    "Quantum history size mismatch for process " + actual.name
            );

            for (int j = 0; j < expectedQH.size(); j++) {
                assertEquals(
                        expectedQH.get(j).getAsInt(),
                        actual.quantumHistory.get(j),
                        "Quantum history mismatch for process " + actual.name + " at index " + j
                );
            }
        }

        //average
        assertEquals(
                expected.get("averageWaitingTime").getAsDouble(),
                result.avgW,
                0.01,
                "Average waiting time mismatch"
        );

        assertEquals(
                expected.get("averageTurnaroundTime").getAsDouble(),
                result.avgT,
                0.01,
                "Average turnaround time mismatch"
        );
    }

    //test cases

    @Test
    void testAG_1() throws Exception {
        runTest("test_cases_v3/AG_test1.json");
    }

    @Test
    void testAG_2() throws Exception {
        runTest("test_cases_v3/AG_test2.json");
    }

    @Test
    void testAG_3() throws Exception {
        runTest("test_cases_v3/AG_test3.json");
    }

    @Test
    void testAG_4() throws Exception {
        runTest("test_cases_v3/AG_test4.json");
    }

    @Test
    void testAG_5() throws Exception {
        runTest("test_cases_v3/AG_test5.json");
    }

    @Test
    void testAG_6() throws Exception {
        runTest("test_cases_v3/AG_test6.json");
    }
    
}
