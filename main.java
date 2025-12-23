import java.util.*;
import java.io.FileReader;
import com.google.gson.Gson;

class Process {
    String name;
    int arrival, burst, priority;
    int remaining, completion, waiting, turnaround;

    Process() {}

    Process(String name, int arrival, int burst) {
        this.name = name;
        this.arrival = arrival;
        this.burst = burst;
        this.remaining = burst;
    }
}

class TestCase {
    String name;
    Input input;
    ExpectedOutput expectedOutput;
}

class Input {
    int contextSwitch;
    int rrQuantum;
    int agingInterval;
    ArrayList<Process> processes;
}

class ExpectedOutput {
    Algorithm SJF;
    Algorithm RR;
    Algorithm Priority;
}

class Algorithm {
    ArrayList<String> executionOrder;
    ArrayList<ProcessResult> processResults;
    double averageWaitingTime;
    double averageTurnaroundTime;
}

class ProcessResult {
    String name;
    int waitingTime;
    int turnaroundTime;
}

public class main {

    public static void main(String[] args) {

        String[] files = {
                "D:/My Desktop/Operating Systems/Operating Systems ass 3/src/test_1.json",
                "D:/My Desktop/Operating Systems/Operating Systems ass 3/src/test_2.json",
                "D:/My Desktop/Operating Systems/Operating Systems ass 3/src/test_3.json",
                "D:/My Desktop/Operating Systems/Operating Systems ass 3/src/test_4.json",
                "D:/My Desktop/Operating Systems/Operating Systems ass 3/src/test_5.json",
                "D:/My Desktop/Operating Systems/Operating Systems ass 3/src/test_6.json"
        };

        for (String file : files) {
            System.out.println("\n==============================");
            System.out.println("Running file: " + file);
            System.out.println("==============================");

            runSJF(file);
        }
    }

    static void runSJF(String fileName) {
        try {
            Gson gson = new Gson();
            FileReader reader = new FileReader(fileName);


            TestCase data = gson.fromJson(reader, TestCase.class);
            ArrayList<Process> list = data.input.processes;
            int contextSwitch = data.input.contextSwitch;

            for (Process p : list) {
                p.remaining = p.burst;
            }

            int processNum = list.size();
            list.sort(Comparator.comparingInt(p -> p.arrival));

            int time = 0;
            int completed = 0;
            Process lastProcess = null;

            while (completed < processNum) {
                Process shortest = null;

                for (Process p : list) {
                    if (p.arrival <= time && p.remaining > 0) {
                        if (shortest == null || p.remaining < shortest.remaining) {
                            shortest = p;
                        }
                    }
                }

                if (shortest == null) {
                    time++;
                    continue;
                }

                if (lastProcess != null && lastProcess != shortest) {
                    time += contextSwitch;
                }

                lastProcess = shortest;
                shortest.remaining--;
                time++;

                if (shortest.remaining == 0) {
                    shortest.completion = time;
                    shortest.turnaround = shortest.completion - shortest.arrival;
                    shortest.waiting = shortest.turnaround - shortest.burst;
                    completed++;
                }
            }

            // ===== Output =====
            for (Process p : list) {
                System.out.println(p.name +
                        " | Waiting: " + p.waiting +
                        " | Turnaround: " + p.turnaround);
            }

            double avgW = list.stream().mapToInt(p -> p.waiting).average().orElse(0);
            double avgT = list.stream().mapToInt(p -> p.turnaround).average().orElse(0);

            System.out.println("Average Waiting Time = " + avgW);
            System.out.println("Average Turnaround Time = " + avgT);

        } catch (Exception e) {
            System.out.println("Error in file " + fileName + ": " + e.getMessage());
        }
    }
}
