import com.google.gson.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileReader;
import java.util.*;


class Process {
    String name;
    int arrival, burst, priority;
    int remaining;
    int completionTime;
    int waitingTime;
    int turnaroundTime;

    Process(String n, int a, int b, int p) {
        name = n;
        arrival = a;
        burst = b;
        priority = p;
        remaining = b;
    }
}


interface Scheduler {
    // Runs the scheduling algorithm
    void run();
    // Returns the order in which processes were executed
    ArrayList<String> getExecutionOrder();
}


public class RoundRobin implements Scheduler {

    // List of processes to schedule
    private final ArrayList<Process> processes;

    // Round Robin parameters
    private final int quantum;
    private final int context;

    private int time = 0;
    private final ArrayList<String> executionOrder = new ArrayList<>();

    RoundRobin(ArrayList<Process> p, int q, int c) {
        processes = p;
        quantum = q;
        context = c;
    }

    public void run() {
        Queue<Process> queue = new LinkedList<>();
        // Sort processes by arrival time
        processes.sort(Comparator.comparingInt(x -> x.arrival));
        int completed = 0;

        // Handle initial idle time until first process arrives
        while (queue.isEmpty()) {
            for (Process p : processes)
                if (p.arrival == time) //if process just arrived add to queue
                    queue.add(p);
            if (queue.isEmpty()) //else increase time
                time++;
        }

        //main loop
        while (completed < processes.size()) {

            Process cur = queue.poll(); //take out first process in queue
            executionOrder.add(cur.name); //execute it

            // Execute for quantum or remaining time
            int run = Math.min(quantum, cur.remaining);
            cur.remaining -= run;
            time += run;

            // Add newly arrived processes
            addArrivals(queue, cur);

            //if process finished
            if (cur.remaining == 0) {
                finishProcess(cur);
                completed++;
            }
            // Otherwise re insert into queue
            else {
                queue.add(cur);
            }

            // Context switching delay
            if (completed < processes.size()) {
                time += context;
                addArrivals(queue, null);
            }
        }
    }

    //Add processes that arrived by current time
    private void addArrivals(Queue<Process> q, Process cur) {
        for (Process p : processes)
            //if process arrived  and still has time to execute and is not in ready queue and not the current process
            if (p.arrival <= time && p.remaining > 0 && !q.contains(p) && p != cur)
                q.add(p); //add to ready queue
    }

    //Compute final process times
    private void finishProcess(Process p) {
        p.completionTime = time;
        p.turnaroundTime = p.completionTime - p.arrival;
        p.waitingTime = p.turnaroundTime - p.burst;
    }

    // Return execution order
    public ArrayList<String> getExecutionOrder() {
        return executionOrder;
    }
    public ArrayList<Process> getProcesses() {
        return processes;
    }
}

//JUnit Test Class for Round Robin Scheduler
class SchedulerJUnitTest {

    @Test
    void testCase1() throws Exception {
        runRRTest("test_cases_v5/Other_Schedulers/test_1.json");
    }

    @Test
    void testCase2() throws Exception {
        runRRTest("test_cases_v5/Other_Schedulers/test_2.json");
    }

    @Test
    void testCase3() throws Exception {
        runRRTest("test_cases_v5/Other_Schedulers/test_3.json");
    }

    @Test
    void testCase4() throws Exception {
        runRRTest("test_cases_v5/Other_Schedulers/test_4.json");
    }

    @Test
    void testCase5() throws Exception {
        runRRTest("test_cases_v5/Other_Schedulers/test_5.json");
    }

    @Test
    void testCase6() throws Exception {
        runRRTest("test_cases_v5/Other_Schedulers/test_6.json");
    }

    //Runs one Round Robin test from a JSON file
    void runRRTest(String path) throws Exception {

        // Read JSON test file
        JsonObject json =
                JsonParser.parseReader(new FileReader(path)).getAsJsonObject();

        // Load and clone processes
        List<Process> base = loadProcesses(json);
        List<Process> processes = clone(base);

        // Read RR parameters
        JsonObject input = json.getAsJsonObject("input");
        int quantum = input.get("rrQuantum").getAsInt();
        int cs = input.get("contextSwitch").getAsInt();

        RoundRobin rr = new RoundRobin(
                new ArrayList<>(processes),
                quantum,
                cs
        );
        rr.run();

        // Expected results
        JsonObject expected = json.getAsJsonObject("expectedOutput")
                .getAsJsonObject("RR");

        //Execution order check
        ArrayList<String> actualOrder = rr.getExecutionOrder();
        JsonArray expectedOrder = expected.getAsJsonArray("executionOrder");

        assertEquals(expectedOrder.size(), actualOrder.size(),
                "Execution order length mismatch in " + path);

        for (int i = 0; i < expectedOrder.size(); i++) {
            assertEquals(
                    expectedOrder.get(i).getAsString(),
                    actualOrder.get(i),
                    "Execution order mismatch at index " + i + " in " + path
            );
        }

        //Process times check
        JsonArray expectedProcesses = expected.getAsJsonArray("processResults");

        for (JsonElement e : expectedProcesses) {
            JsonObject ep = e.getAsJsonObject();

            Process p = rr.getProcesses().stream()
                    .filter(x -> x.name.equals(ep.get("name").getAsString()))
                    .findFirst()
                    .orElseThrow();

            assertEquals(ep.get("waitingTime").getAsInt(), p.waitingTime,
                    "Waiting time mismatch for " + p.name);

            assertEquals(ep.get("turnaroundTime").getAsInt(), p.turnaroundTime,
                    "Turnaround time mismatch for " + p.name);
        }

        //average time check
        double aw = rr.getProcesses().stream()
                .mapToInt(p -> p.waitingTime)
                .average()
                .orElse(0);

        double at = rr.getProcesses().stream()
                .mapToInt(p -> p.turnaroundTime)
                .average()
                .orElse(0);


        assertEquals(expected.get("averageWaitingTime").getAsDouble(), aw, 0.01);
        assertEquals(expected.get("averageTurnaroundTime").getAsDouble(), at, 0.01);
    }


    // Load processes from JSON input
    static List<Process> loadProcesses(JsonObject json) {
        List<Process> list = new ArrayList<>();
        JsonArray arr = json.getAsJsonObject("input").getAsJsonArray("processes");

        for (JsonElement e : arr) {
            JsonObject p = e.getAsJsonObject();
            list.add(new Process(
                    p.get("name").getAsString(),
                    p.get("arrival").getAsInt(),
                    p.get("burst").getAsInt(),
                    p.get("priority").getAsInt()
            ));
        }
        return list;
    }

    //copy of process list
    static List<Process> clone(List<Process> src) {
        List<Process> copy = new ArrayList<>();
        for (Process p : src)
            copy.add(new Process(p.name, p.arrival, p.burst, p.priority));
        return copy;
    }
}
class RoundRobinMainRunner {

    public static void main(String[] args) throws Exception {

        String[] tests = {
                "test_cases_v5/Other_Schedulers/test_1.json",
                "test_cases_v5/Other_Schedulers/test_2.json",
                "test_cases_v5/Other_Schedulers/test_3.json",
                "test_cases_v5/Other_Schedulers/test_4.json",
                "test_cases_v5/Other_Schedulers/test_5.json",
                "test_cases_v5/Other_Schedulers/test_6.json"
        };

        for (String path : tests) {
            runAndPrint(path);
        }
    }

    static void runAndPrint(String path) throws Exception {

        System.out.println("Test file: " + path);

        JsonObject json =
                JsonParser.parseReader(new FileReader(path)).getAsJsonObject();

        // Load processes
        List<Process> processes = loadProcesses(json);

        // Read RR parameters
        JsonObject input = json.getAsJsonObject("input");
        int quantum = input.get("rrQuantum").getAsInt();
        int cs = input.get("contextSwitch").getAsInt();

        // Run Round Robin
        RoundRobin rr = new RoundRobin(
                new ArrayList<>(processes),
                quantum,
                cs
        );
        rr.run();

        // output
        System.out.println("\nProcesses execution order:");
        System.out.println(rr.getExecutionOrder());

        double totalWT = 0;
        double totalTT = 0;

        for (Process p : rr.getProcesses()) {
            System.out.printf("%-10s %-15d %-20d%n",
                    p.name, p.waitingTime, p.turnaroundTime);

            totalWT += p.waitingTime;
            totalTT += p.turnaroundTime;
        }

        System.out.printf("\nAverage Waiting Time    = %.2f%n",
                totalWT / processes.size());

        System.out.printf("Average Turnaround Time = %.2f%n\n",
                totalTT / processes.size());
    }

    static List<Process> loadProcesses(JsonObject json) {
        List<Process> list = new ArrayList<>();
        JsonArray arr = json.getAsJsonObject("input").getAsJsonArray("processes");

        for (JsonElement e : arr) {
            JsonObject p = e.getAsJsonObject();
            list.add(new Process(
                    p.get("name").getAsString(),
                    p.get("arrival").getAsInt(),
                    p.get("burst").getAsInt(),
                    p.get("priority").getAsInt()
            ));
        }
        return list;
    }
}

