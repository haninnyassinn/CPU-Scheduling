import com.google.gson.*;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestRunner {
    //helper method to run and compare
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
