import com.google.gson.*;
import java.io.FileReader;
import java.util.*;

/* =========================
   Process
   ========================= */
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

/* =========================
   Scheduler Interface
   ========================= */
interface Scheduler {
    void run();
    ArrayList<String> getExecutionOrder();
}

/* =========================
   Round Robin Scheduler
   ========================= */
class RoundRobin implements Scheduler {

    private ArrayList<Process> processes;
    private int quantum, context;
    private int time = 0;
    private ArrayList<String> executionOrder = new ArrayList<>();

    RoundRobin(ArrayList<Process> p, int q, int c) {
        processes = p;
        quantum = q;
        context = c;
    }

    public void run() {
        Queue<Process> queue = new LinkedList<>();
        processes.sort(Comparator.comparingInt(x -> x.arrival));
        int completed = 0;

        while (queue.isEmpty()) {
            for (Process p : processes)
                if (p.arrival == time) queue.add(p);
            if (queue.isEmpty()) time++;
        }

        while (completed < processes.size()) {

            Process cur = queue.poll();
            executionOrder.add(cur.name);

            int run = Math.min(quantum, cur.remaining);
            cur.remaining -= run;
            time += run;

            addArrivals(queue, cur);

            if (cur.remaining == 0) {
                finishProcess(cur);
                completed++;
            } else {
                queue.add(cur);
            }

            if (completed < processes.size()) {
                time += context;
                addArrivals(queue, null);
            }
        }
    }

    private void addArrivals(Queue<Process> q, Process cur) {
        for (Process p : processes)
            if (p.arrival <= time && p.remaining > 0 &&
                    !q.contains(p) && p != cur)
                q.add(p);
    }

    private void finishProcess(Process p) {
        p.completionTime = time;
        p.turnaroundTime = p.completionTime - p.arrival;
        p.waitingTime = p.turnaroundTime - p.burst;
    }

    public ArrayList<String> getExecutionOrder() {
        return executionOrder;
    }
}

/* =========================
   SJF (Non-Preemptive)
   ========================= */
//class SJF implements Scheduler {
//
//
//    public ArrayList<String> getExecutionOrder() {
//        return executionOrder;
//    }
//}

/* =========================
   Priority (Non-Preemptive)
   ========================= */
//class PriorityScheduler implements Scheduler {
//
//   public ArrayList<String> getExecutionOrder() {
//        return executionOrder;
//    }
//}

/* =========================================================
   Scheduler Test Runner
   ========================================================= */
class SchedulerTestRunner {

    public static void main(String[] args) {

        String[] tests = {
                "test_cases_v5/Other_Schedulers/test_1.json",
                "test_cases_v5/Other_Schedulers/test_2.json",
                "test_cases_v5/Other_Schedulers/test_3.json",
                "test_cases_v5/Other_Schedulers/test_4.json",
                "test_cases_v5/Other_Schedulers/test_5.json",
                "test_cases_v5/Other_Schedulers/test_6.json",
        };

        for (String path : tests) {
            runTest(path);
        }
    }

    static void runTest(String path) {
        System.out.println("\n=================================");
        System.out.println("RUNNING TEST: " + path);
        System.out.println("=================================");

        try {
            JsonObject json =
                    JsonParser.parseReader(new FileReader(path)).getAsJsonObject();

            List<Process> base = loadProcesses(json);

            runRR(json, clone(base));
            runSJF(json, clone(base));
            runPriority(json, clone(base));

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    /* ======================= RR ======================= */
    static boolean runRR(JsonObject json, List<Process> processes) {

        JsonObject input = json.getAsJsonObject("input");
        int quantum = input.get("rrQuantum").getAsInt();
        int cs = input.get("contextSwitch").getAsInt();

        RoundRobin rr = new RoundRobin(
                new ArrayList<>(processes),
                quantum,
                cs
        );
        rr.run();

        JsonObject expected = json.getAsJsonObject("expectedOutput")
                .getAsJsonObject("RR");

        boolean pass = true;

        // ================= Execution Order =================
        ArrayList<String> actualOrder = rr.getExecutionOrder();
        JsonArray expectedOrder = expected.getAsJsonArray("executionOrder");

        if (actualOrder.size() != expectedOrder.size()) {
            pass = false;
        } else {
            for (int i = 0; i < expectedOrder.size(); i++) {
                if (!actualOrder.get(i)
                        .equals(expectedOrder.get(i).getAsString())) {
                    pass = false;
                    break;
                }
            }
        }

        // ================= Process Times =================
        JsonArray expectedProcesses = expected.getAsJsonArray("processResults");

        for (JsonElement e : expectedProcesses) {
            JsonObject ep = e.getAsJsonObject();
            String name = ep.get("name").getAsString();
            int ew = ep.get("waitingTime").getAsInt();
            int et = ep.get("turnaroundTime").getAsInt();

            Process p = processes.stream()
                    .filter(x -> x.name.equals(name))
                    .findFirst()
                    .orElse(null);

            if (p == null || p.waitingTime != ew || p.turnaroundTime != et) {
                pass = false;
            }
        }

        // ================= Averages =================
        double aw = processes.stream().mapToInt(p -> p.waitingTime).average().orElse(0);
        double at = processes.stream().mapToInt(p -> p.turnaroundTime).average().orElse(0);

        double ew = expected.get("averageWaitingTime").getAsDouble();
        double et = expected.get("averageTurnaroundTime").getAsDouble();

        if (Math.abs(aw - ew) > 0.01 || Math.abs(at - et) > 0.01) {
            pass = false;
        }

        // ================= OUTPUT =================
        printResults("Round Robin", actualOrder, processes);

        System.out.println(pass ? "✅ RESULT: PASS" : "❌ RESULT: FAIL");

        return pass;
    }


    /* ======================= SJF ======================= */

    static boolean runSJF(JsonObject json, List<Process> processes) {

        SJF sjf = new SJF(new ArrayList<>(processes));
        sjf.run();

        JsonObject expected = json.getAsJsonObject("expectedOutput")
                .getAsJsonObject("SJF");

        boolean pass = validate(expected, sjf.getExecutionOrder(), processes);

        printResults("SJF", sjf.getExecutionOrder(), processes);
        System.out.println(pass ? "✅ RESULT: PASS" : "❌ RESULT: FAIL");

        return pass;
    }


    /* ======================= Priority ======================= */

    static boolean runPriority(JsonObject json, List<Process> processes) {

        PriorityScheduler ps = new PriorityScheduler(new ArrayList<>(processes));
        ps.run();

        JsonObject expected = json.getAsJsonObject("expectedOutput")
                .getAsJsonObject("Priority");

        boolean pass = validate(expected, ps.getExecutionOrder(), processes);

        printResults("Priority", ps.getExecutionOrder(), processes);
        System.out.println(pass ? "✅ RESULT: PASS" : "❌ RESULT: FAIL");

        return pass;
    }


    /* ======================= OUTPUT ======================= */
    static void printResults(String name, ArrayList<String> order, List<Process> p) {

        System.out.println("\n==== " + name + " ====");
        System.out.println("Execution Order: " + order);

        double tw = 0, tt = 0;

        System.out.printf("%-8s %-14s %-18s%n",
                "Process", "Waiting Time", "Turnaround Time");

        for (Process x : p) {
            System.out.printf("%-8s %-14d %-18d%n",
                    x.name, x.waitingTime, x.turnaroundTime);
            tw += x.waitingTime;
            tt += x.turnaroundTime;
        }

        System.out.printf("Average Waiting Time    = %.2f%n", tw / p.size());
        System.out.printf("Average Turnaround Time = %.2f%n", tt / p.size());
    }

    /* ======================= HELPERS ======================= */
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

    static List<Process> clone(List<Process> src) {
        List<Process> copy = new ArrayList<>();
        for (Process p : src)
            copy.add(new Process(p.name, p.arrival, p.burst, p.priority));
        return copy;
    }
}
