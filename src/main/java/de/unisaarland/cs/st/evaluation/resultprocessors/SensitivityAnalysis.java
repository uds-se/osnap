package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

public class SensitivityAnalysis implements IResultProcessor {
    private final String DATA_ROW_FORMAT = "%s, " + // Timing
	    "%s, " + // Final Costs
	    "%d, %d"; // Resource Usage x 2 - Orig -
		     // OnLine -
		     // OffLine
    // Output
    private final String FILE_NAME = "%s.summary.csv";

    private Entry<Integer, Integer> computeResourceUsage(Schedule schedule) {
	int reserved = 0;
	int onDemand = 0;

	for (Instance instance : schedule.jobsDistribution.keySet()) {

	    if (instance.isReserved()) {
		reserved++;
	    } else {
		// Account this only if there's jobs in it

		// Add only if not empty !
		if (schedule.jobsDistribution.get(instance).size() > 0) {
		    onDemand++;
		}
	    }
	}
	return new AbstractMap.SimpleEntry<Integer, Integer>(reserved, onDemand);
    }

    @Override
    public void process(Result result) {

	List<String> onLinePlanners = new ArrayList<String>();

	// Basic planners name do not contain "With"
	for (String plannerName : result.getPlannerNames()) {
	    if (plannerName.contains("With") && !plannerName.contains("OffLine")) {
		onLinePlanners.add(plannerName);
	    }
	}
	// Output a CSV file for each basic planner for OSNAP
	for (String planner : onLinePlanners) {
	    File outputFile = new File(String.format(FILE_NAME, planner));
	    outputFile.delete();

	    try (FileOutputStream fos = new FileOutputStream(outputFile); PrintStream ps = new PrintStream(fos);) {
		// Absolute Time Planners
		Schedule onLine = result.getSchedules().get(planner);
		//
		long absoluteExecutionTimeOnLinePlanner = onLine.getFinalTime();
		// Resource usage
		Entry<Integer, Integer> resourceUsageOnLine = computeResourceUsage(onLine);
		// Costs
		long costOnLine = onLine.getFinalCost();

		// Print the csv line entry
		ps.println(String.format(DATA_ROW_FORMAT, //
			absoluteExecutionTimeOnLinePlanner, //
			costOnLine, resourceUsageOnLine.getKey(), resourceUsageOnLine.getValue()

		));
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }
}
