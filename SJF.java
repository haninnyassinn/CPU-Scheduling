import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileReader;
import java.util.*;

/* ===================== MODELS ===================== */

class Process {
    String name;
    int arrival, burst, priority;
    int remaining;
    int completion;
    int waiting;
    int turnaround;

    Process() {} // Gson

    Process(String n, int a, int b) {
        name = n;
        arrival = a;
        burst = b;
        remaining = b;
    }
}

class TestCase {
    Input input;
    ExpectedOutput expectedOutput;
}

class Input {
    int contextSwitch;
    ArrayList<Process> processes;
}

class ExpectedOutput {
    Algorithm SJF;
}

class Algorithm {
    ArrayList<ProcessResult> processResults;
    double averageWaitingTime;
    double averageTurnaroundTime;
}

class ProcessResult {
    String name;
    int waitingTime;
    int turnaroundTime;
}

/* ===================== SJF SCHEDULER ===================== */

class SJFScheduler {

    static ArrayList<Process> run(String path) throws Exception {

        Gson gson = new Gson();
        TestCase data =
                gson.fromJson(new FileReader(path), TestCase.class);

        ArrayList<Process> list = data.input.processes;
        int context = data.input.contextSwitch;

        for (Process p : list)
            p.remaining = p.burst;

        list.sort(Comparator.comparingInt(p -> p.arrival));

        int time = 0, completed = 0;
        Process last = null;

        while (completed < list.size()) {

            Process shortest = null;

            for (Process p : list)
                if (p.arrival <= time && p.remaining > 0)
                    if (shortest == null || p.remaining < shortest.remaining)
                        shortest = p;

            if (shortest == null) {
                time++;
                continue;
            }

            if (last != null && last != shortest)
                time += context;

            last = shortest;
            shortest.remaining--;
            time++;

            if (shortest.remaining == 0) {
                shortest.completion = time;
                shortest.turnaround = time - shortest.arrival;
                shortest.waiting = shortest.turnaround - shortest.burst;
                completed++;
            }
        }

        return list;
    }
}

/* ===================== JUNIT TEST ===================== */

class SJFJUnitTest {

    @Test
    void testCase1() throws Exception {
        runTest("test_cases_v5/Other_Schedulers/test_1.json");
    }

    @Test
    void testCase2() throws Exception {
        runTest("test_cases_v5/Other_Schedulers/test_2.json");
    }

    @Test
    void testCase3() throws Exception {
        runTest("test_cases_v5/Other_Schedulers/test_3.json");
    }

    @Test
    void testCase4() throws Exception {
        runTest("test_cases_v5/Other_Schedulers/test_4.json");
    }

    @Test
    void testCase5() throws Exception {
        runTest("test_cases_v5/Other_Schedulers/test_5.json");
    }

    @Test
    void testCase6() throws Exception {
        runTest("test_cases_v5/Other_Schedulers/test_6.json");
    }

    void runTest(String path) throws Exception {

        JsonObject json =
                JsonParser.parseReader(new FileReader(path)).getAsJsonObject();

        ArrayList<Process> result = SJFScheduler.run(path);

        Algorithm expected =
                new Gson().fromJson(
                        json.getAsJsonObject("expectedOutput")
                                .getAsJsonObject("SJF"),
                        Algorithm.class
                );

        // ---- assert per process ----
        for (ProcessResult pr : expected.processResults) {

            Process p = result.stream()
                    .filter(x -> x.name.equals(pr.name))
                    .findFirst()
                    .orElseThrow();

            assertEquals(pr.waitingTime, p.waiting,
                    "Waiting time mismatch for " + p.name);

            assertEquals(pr.turnaroundTime, p.turnaround,
                    "Turnaround time mismatch for " + p.name);
        }

        // ---- average asserts ----
        double aw = result.stream().mapToInt(p -> p.waiting).average().orElse(0);
        double at = result.stream().mapToInt(p -> p.turnaround).average().orElse(0);

        assertEquals(expected.averageWaitingTime, aw, 0.01);
        assertEquals(expected.averageTurnaroundTime, at, 0.01);
    }
}

/* ===================== MAIN ===================== */

public class SJF {

    public static void main(String[] args) throws Exception {

        String path =
                "D:/My Desktop/Operating Systems/Operating Systems ass 3/src/test_1.json";

        ArrayList<Process> list = SJFScheduler.run(path);

        System.out.println("Process | Waiting | Turnaround");
        for (Process p : list)
            System.out.println(p.name + " | " + p.waiting + " | " + p.turnaround);
    }
}
