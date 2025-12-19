import java.util.*;
import java.io.*;
import com.google.gson.*;

class Process {
    String name;
    int arrival, burst, priority;
    int remaining, completion, waiting, turnaround;

    Process() {} // Needed for Gson

    Process(String name, int arrival, int burst, int priority) {
        this.name = name;
        this.arrival = arrival;
        this.burst = burst;
        this.priority = priority;
        this.remaining = burst;
        waiting = 0;
    }
}

interface Scheduler {
    void run();
    ArrayList<String> getExecutionOrder();
    ArrayList<Process> getProcesses();
}


class RoundRobin implements Scheduler{

    private ArrayList<Process> processes;  // All processes
    private int quantum;
    private int context;
    private int time = 0;


    private ArrayList<String> executionOrder = new ArrayList<>(); //stores the order of processes

    //constructor
    public RoundRobin(ArrayList<Process> processes, int quantum, int context) {
        this.processes = processes;
        this.quantum = quantum;
        this.context = context;
    }

    public void run() {
        Queue<Process> queue = new LinkedList<>(); // Ready queue for processes
        int completed = 0; // Number of completed processes
        int n = processes.size();  // Total number of processes

        //sort processes by arrival time
        processes.sort(Comparator.comparingInt(p -> p.arrival));

        // Handle initial idle time
        while (queue.isEmpty()) { //while no process is ready
            for (Process p : processes) {
                if (p.arrival == time)  //if process arrives now
                    queue.add(p);  //add it to ready queue
            }
            if (queue.isEmpty())  //if no process is ready
                time++; //move time by 1

        }

        while (completed < n) {
            Process current = queue.poll();   // Remove next process from ready queue
            executionOrder.add(current.name);  //record execution order

            int run = Math.min(quantum, current.remaining);  //calculate execution time
            current.remaining -= run;  //reduce remaining time of process
            time += run;

            addArrivals(queue, current); //add any new process arrived during execution

            //if process finished execution
            if (current.remaining == 0) {
                current.turnaround = time - current.arrival;
                current.waiting = current.turnaround - current.burst;
                completed++;
            }
            // If not finished, put process back into ready queue
            else {
                queue.add(current);
            }

            //handle context switching
            if (completed < n) {
                time += context;
                addArrivals(queue, null); //add arrivals during context switching
            }
        }
    }
    // Add processes that arrive by current time
    private void addArrivals(Queue<Process> queue, Process current) {
        for (Process p : processes) {
            if (p.arrival <= time && p.remaining > 0 &&
                    !queue.contains(p) && p != current) {
                queue.add(p);
            }
        }
    }

    public ArrayList<String> getExecutionOrder() { return executionOrder; }
    public ArrayList<Process> getProcesses() { return processes; }
}


class TestCase {
    String name;
    Input input;
    ExpectedOutput expectedOutput;

    static class Input {
        int contextSwitch;
        int rrQuantum;
        ArrayList<Process> processes;
    }
    static class ExpectedOutput {
        Algorithm RR;
    }
    static class Algorithm {
        ArrayList<String> executionOrder;
    }
}

class TestRunner {

    public static void runRRTest(TestCase tc) {
        ArrayList<Process> copy = new ArrayList<>();
        for (Process p : tc.input.processes) {
            copy.add(new Process(p.name, p.arrival, p.burst, p.priority));
        }

        RoundRobin rr = new RoundRobin(copy, tc.input.rrQuantum, tc.input.contextSwitch);
        rr.run();

        ArrayList<String> actual = rr.getExecutionOrder();
        ArrayList<String> expected = tc.expectedOutput.RR.executionOrder;

        System.out.println("\n=== " + tc.name + " (RR) ===");
        System.out.println("Expected: " + expected);
        System.out.println("Actual : " + actual);

        if (actual.equals(expected)) {
            System.out.println("RESULT : PASS");
        } else {
            System.out.println("RESULT : FAIL");
        }
    }
}
public class Main {

    public static void main(String[] args) {

        try {
            Gson gson = new Gson();
            Reader reader = new FileReader("test_cases_v3/Other_Schedulers/test_1.json");
            TestCase tc = gson.fromJson(reader, TestCase.class);
            TestRunner.runRRTest(tc);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
}
}
