import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileReader;
import java.util.*;

/* =========================================================
   1. Process Entity
   ========================================================= */
class Process {
    String name;
    int arrivalTime;
    int burstTime;
    int remainingTime;
    int priority;

    int waitingTime = 0;
    int turnaroundTime = 0;
    boolean finished = false;

    int last; // last time aging was applied

    Process(String name, int arrival, int burst, int priority) {
        this.name = name;
        this.arrivalTime = arrival;
        this.burstTime = burst;
        this.remainingTime = burst;
        this.priority = priority;
        this.last = arrival;
    }
}

/* =========================================================
   2. Priority Preemptive Scheduler Logic
   ========================================================= */
public class PriorityPreemptiveScheduler {
    private final List<Process> processes;
    private final int contextSwitch;
    private final int agingInterval;

    private int time = 0;
    private Process running = null;
    private int csRemaining = 0;
    private Process csTarget = null;

    private final List<String> executionOrder = new ArrayList<>();

    PriorityPreemptiveScheduler(List<Process> processes, int cs, int aging) {
        this.processes = processes;
        this.contextSwitch = cs;
        this.agingInterval = aging;
        schedule();
    }

    private void schedule() {
        while (!allFinished()) {
            // Context switch delay
            if (csRemaining > 0) {
                incrementWaiting(null);
                time++;
                csRemaining--;
                if (csRemaining == 0) running = csTarget;
                continue;
            }

            List<Process> ready = getReady();

            if (ready.isEmpty()) {
                if (executionOrder.isEmpty() || !executionOrder.get(executionOrder.size() - 1).equals("IDLE")) {
                    executionOrder.add("IDLE");
                }
                time++;
                continue;
            }

            applyAging(ready);
            Process selected = selectProcess(ready);

            if (running != selected) {
                executionOrder.add(selected.name);
                csTarget = selected;
                csRemaining = contextSwitch;
                if (contextSwitch == 0) running = selected;
                continue;
            }

            executeOneUnit(running);
        }
    }

    private void executeOneUnit(Process p) {
        incrementWaiting(p);
        p.remainingTime--;
        time++;

        if (p.remainingTime == 0) {
            p.finished = true;
            p.turnaroundTime = time - p.arrivalTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;
            running = null;
        }
    }

    private void incrementWaiting(Process exclude) {
        for (Process p : processes) {
            if (!p.finished && p != exclude && p.arrivalTime <= time) {
                p.waitingTime++;
            }
        }
    }

    private void applyAging(List<Process> ready) {
        for (Process p : ready) {
            if (p == running) continue;
            if (time - p.last >= agingInterval) {
                p.priority = Math.max(1, p.priority - 1);
                p.last = time;
            }
        }
    }

    private Process selectProcess(List<Process> ready) {
        return ready.stream()
                .min(Comparator
                        .comparingInt((Process p) -> p.priority)
                        .thenComparingInt(p -> p.arrivalTime))
                .orElse(null);
    }

    private List<Process> getReady() {
        List<Process> r = new ArrayList<>();
        for (Process p : processes)
            if (!p.finished && p.arrivalTime <= time)
                r.add(p);
        return r;
    }

    private boolean allFinished() {
        for (Process p : processes)
            if (!p.finished) return false;
        return true;
    }

    List<String> getExecutionOrder() { return executionOrder; }
    List<Process> getProcesses() { return processes; }
}

/* =========================================================
   3. JUnit Test Runner (JSON Driven)
   ========================================================= */
 class PriorityPreemptiveJUnitTest {

    @Test
    void testCase1() throws Exception {
        runPriorityTest("test_cases_v5/Other_Schedulers/test_1.json");
    }

    @Test
    void testCase2() throws Exception {
        runPriorityTest("test_cases_v5/Other_Schedulers/test_2.json");
    }

    void runPriorityTest(String path) throws Exception {
        JsonObject json = JsonParser.parseReader(new FileReader(path)).getAsJsonObject();

        // Load processes
        List<Process> processes = loadProcesses(json);

        // Parameters
        JsonObject input = json.getAsJsonObject("input");
        int cs = input.get("contextSwitch").getAsInt();
        int aging = input.get("agingInterval").getAsInt();

        // Run Scheduler
        PriorityPreemptiveScheduler scheduler = new PriorityPreemptiveScheduler(processes, cs, aging);

        // Assertions
        JsonObject expected = json.getAsJsonObject("expectedOutput").getAsJsonObject("Priority");

        // 1. Order Check
        List<String> actualOrder = scheduler.getExecutionOrder();
        JsonArray expectedOrder = expected.getAsJsonArray("executionOrder");
        assertEquals(expectedOrder.size(), actualOrder.size(), "Order length mismatch in " + path);
        for (int i = 0; i < expectedOrder.size(); i++) {
            assertEquals(expectedOrder.get(i).getAsString(), actualOrder.get(i), "Mismatch at index " + i);
        }

        // 2. Process Metrics Check
        JsonArray expectedResults = expected.getAsJsonArray("processResults");
        for (JsonElement e : expectedResults) {
            JsonObject ep = e.getAsJsonObject();
            Process p = scheduler.getProcesses().stream()
                    .filter(x -> x.name.equals(ep.get("name").getAsString()))
                    .findFirst().orElseThrow();

            assertEquals(ep.get("waitingTime").getAsInt(), p.waitingTime, "WT mismatch: " + p.name);
            assertEquals(ep.get("turnaroundTime").getAsInt(), p.turnaroundTime, "TAT mismatch: " + p.name);
        }

        // 3. Averages Check
        double avgWT = scheduler.getProcesses().stream().mapToInt(p -> p.waitingTime).average().orElse(0);
        double avgTAT = scheduler.getProcesses().stream().mapToInt(p -> p.turnaroundTime).average().orElse(0);

        assertEquals(expected.get("averageWaitingTime").getAsDouble(), avgWT, 0.01);
        assertEquals(expected.get("averageTurnaroundTime").getAsDouble(), avgTAT, 0.01);
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