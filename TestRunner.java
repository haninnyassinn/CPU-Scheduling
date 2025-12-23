import com.google.gson.*;
import java.io.FileReader;
import java.util.*;

public class TestRunner {

    public static void main(String[] args) {
        System.out.println("testing:");

        boolean allTestsPassed = true;

        // Run all test cases
        allTestsPassed &= runTest("test_cases_v3/AG_test1.json");
        allTestsPassed &= runTest("test_cases_v3/AG_test2.json");
        allTestsPassed &= runTest("test_cases_v3/AG_test3.json");
        allTestsPassed &= runTest("test_cases_v3/AG_test4.json");
        allTestsPassed &= runTest("test_cases_v3/AG_test5.json");
        allTestsPassed &= runTest("test_cases_v3/AG_test6.json");

        System.out.println("finished testing result:");
        if (allTestsPassed) {
            System.out.println("all 6 tests passed");
        } else {
            System.out.println("some tests failed");
        }
    }

    static boolean runTest(String path) {
        System.out.println("=".repeat(60));
        System.out.println("RUNNING TEST: " + path);
        System.out.println("=".repeat(60));

        try {
            // read JSON file
            JsonObject json = JsonParser.parseReader(new FileReader(path)).getAsJsonObject();

            // get input processes
            JsonArray arr = json.getAsJsonObject("input").getAsJsonArray("processes");
            List<AGScheduler.Process> processes = new ArrayList<>();

            for (JsonElement e : arr) {
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
            JsonObject expected = json.getAsJsonObject("expectedOutput");

            System.out.println("\ncompare results:");
            boolean testPassed = true;

            // compare execution order
            System.out.println("Processes execution order:");
            JsonArray expectedOrder = expected.get("executionOrder").getAsJsonArray();
            System.out.println("Expected: " + expectedOrder);
            System.out.println("the code: " + result.order);

            if (result.order.size() != expectedOrder.size()) {
                System.out.println("not same size");
                testPassed = false;
            } else {
                boolean orderMatches = true;
                for (int i = 0; i < expectedOrder.size(); i++) {
                    if (!result.order.get(i).equals(expectedOrder.get(i).getAsString())) {
                        orderMatches = false;
                        break;
                    }
                }
                if (orderMatches) {
                    System.out.println("order matches");
                } else {
                    System.out.println("order doesn't match");
                    testPassed = false;
                }
            }

            // compare waiting times
            System.out.println("\n Waiting Time for each process:");
            JsonArray expectedProcesses = expected.get("processResults").getAsJsonArray();

            boolean waitingTimesMatch = true;
            for (int i = 0; i < result.processResults.size(); i++) {
                AGScheduler.ProcessResult actual = result.processResults.get(i);
                JsonObject expectedProc = expectedProcesses.get(i).getAsJsonObject();

                int expectedWT = expectedProc.get("waitingTime").getAsInt();
                System.out.printf("%s: Expected=%d, code=%d",
                        actual.name, expectedWT, actual.waitingTime);

                if (actual.waitingTime == expectedWT) {
                    System.out.println(" match");
                } else {
                    System.out.println(" does not match");
                    waitingTimesMatch = false;
                }
            }
            if (!waitingTimesMatch) testPassed = false;

            // 3. Compare turnaround times
            System.out.println("\nTurnaround Time for each process:");
            boolean turnaroundTimesMatch = true;
            for (int i = 0; i < result.processResults.size(); i++) {
                AGScheduler.ProcessResult actual = result.processResults.get(i);
                JsonObject expectedProc = expectedProcesses.get(i).getAsJsonObject();

                int expectedTT = expectedProc.get("turnaroundTime").getAsInt();
                System.out.printf("%s: Expected=%d, my code=%d",
                        actual.name, expectedTT, actual.turnaroundTime);

                if (actual.turnaroundTime == expectedTT) {
                    System.out.println(" match");
                } else {
                    System.out.println(" does not match");
                    turnaroundTimesMatch = false;
                }
            }
            if (!turnaroundTimesMatch) testPassed = false;

            // Compare average waiting time
            System.out.println("\nAverage Waiting Time:");
            double expectedAvgW = expected.get("averageWaitingTime").getAsDouble();
            System.out.printf("Expected: %.2f\n", expectedAvgW);
            System.out.printf("my code: %.2f\n", result.avgW);

            if (Math.abs(result.avgW - expectedAvgW) <= 0.01) {
                System.out.println("   matches");
            } else {
                System.out.println("   does not match");
                testPassed = false;
            }

            // Compare average turnaround time
            System.out.println("\nAverage Turnaround Time:");
            double expectedAvgT = expected.get("averageTurnaroundTime").getAsDouble();
            System.out.printf("Expected: %.2f\n", expectedAvgT);
            System.out.printf("my code: %.2f\n", result.avgT);

            if (Math.abs(result.avgT - expectedAvgT) <= 0.01) {
                System.out.println("    matches");
            } else {
                System.out.println("    does not match");
                testPassed = false;
            }

            //Compare quantum history
            System.out.println("\nQuantum history for each process:");
            boolean quantumHistoryMatches = true;
            for (int i = 0; i < result.processResults.size(); i++) {
                AGScheduler.ProcessResult actual = result.processResults.get(i);
                JsonObject expectedProc = expectedProcesses.get(i).getAsJsonObject();
                JsonArray expectedQH = expectedProc.get("quantumHistory").getAsJsonArray();

                System.out.println("   " + actual.name + ":");
                System.out.println("Expected: " + expectedQH);
                System.out.println("my code: " + actual.quantumHistory);

                if (actual.quantumHistory.size() != expectedQH.size()) {
                    System.out.println("size not equal");
                    quantumHistoryMatches = false;
                } else {
                    boolean qhMatch = true;
                    for (int j = 0; j < expectedQH.size(); j++) {
                        if (actual.quantumHistory.get(j) != expectedQH.get(j).getAsInt()) {
                            qhMatch = false;
                            break;
                        }
                    }
                    if (qhMatch) {
                        System.out.println(" matches");
                    } else {
                        System.out.println("  does not match");
                        quantumHistoryMatches = false;
                    }
                }
            }
            if (!quantumHistoryMatches) testPassed = false;

            // display final test result
            System.out.println("final result is: ");
            if (testPassed) {
                System.out.println(" test passed");
            } else {
                System.out.println(" test failed");
            }

            return testPassed;

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            return false;
        }
    }
}