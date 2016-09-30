package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

public class Experiment2 implements IResultProcessor {
    private final String DATA_ROW_FORMAT = "%s,%s,%.3f,%s,%.3f," + // Timing
	    "%s,%s,%s," + // Final Costs
	    "%d,%d,%d,%d,%d,%d"; // Resource Usage x 2 - Orig -
				 // OnLine -
				 // OffLine
    // Output
    private final String FILE_NAME = "%s.summary.csv";
    private final String FILE_NAME_FAST = "%s-FAST.summary.csv";

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
	Set<String> basicPlanners = new HashSet<String>();
	Map<String, String> onLinePlanners = new HashMap<String, String>();
	Map<String, String> onLinePlannersFAST = new HashMap<String, String>();
	Map<String, String> offLinePlanners = new HashMap<String, String>();
	Map<String, String> offLinePlannersFAST = new HashMap<String, String>();

	// Basic planners name do not contain "With"
	for (String plannerName : result.getPlannerNames()) {
	    if (!plannerName.contains("With")) {
		basicPlanners.add(plannerName);
	    } else if (!plannerName.contains("OffLine")) {
		String basicPlanner = plannerName.substring(0, plannerName.lastIndexOf("With"));
		if (plannerName.contains("Fast")) {
		    onLinePlannersFAST.put(basicPlanner, plannerName);
		} else {
		    onLinePlanners.put(basicPlanner, plannerName);
		}
	    } else {
		String basicPlanner = plannerName.substring(0, plannerName.lastIndexOf("With"));
		if (plannerName.contains("Fast")) {
		    offLinePlannersFAST.put(basicPlanner, plannerName);
		} else {
		    offLinePlanners.put(basicPlanner, plannerName);
		}
	    }
	}
	// System.err.println(basicPlanners);
	// System.err.println(onLinePlanners);
	// System.err.println(offLinePlanners);

	// Output a CSV file for each basic planner for OSNAP
	for (String basicPlanner : basicPlanners) {
	    File outputFile = new File(String.format(FILE_NAME, basicPlanner));
	    outputFile.delete();

	    if (!offLinePlanners.containsKey(basicPlanner))
		continue;

	    if (!onLinePlanners.containsKey(basicPlanner))
		continue;

	    try (FileOutputStream fos = new FileOutputStream(outputFile); PrintStream ps = new PrintStream(fos);) {
		// Absolute Time Planners

		Schedule original = result.getSchedules().get(basicPlanner);
		Schedule onLine = result.getSchedules().get(onLinePlanners.get(basicPlanner));
		Schedule offLine = result.getSchedules().get(offLinePlanners.get(basicPlanner));

		long absoluteExecutionTimeBasicPlanner = original.getFinalTime();
		long absoluteExecutionTimeOnLinePlanner = onLine.getFinalTime();
		long absoluteExecutionTimeOffLinePlanner = offLine.getFinalTime();
		// Relative speedup/down
		double relativeExecutionTimeOnLinePlanner = ((double) (absoluteExecutionTimeBasicPlanner
			- absoluteExecutionTimeOnLinePlanner)) / absoluteExecutionTimeBasicPlanner * 100;
		double relativeExecutionTimeOffLinePlanner = ((double) (absoluteExecutionTimeBasicPlanner
			- absoluteExecutionTimeOffLinePlanner)) / absoluteExecutionTimeBasicPlanner * 100;

		// Resource usage
		Entry<Integer, Integer> resourceUsage = computeResourceUsage(original);
		Entry<Integer, Integer> resourceUsageOnLine = computeResourceUsage(onLine);
		Entry<Integer, Integer> resourceUsageOffLine = computeResourceUsage(offLine);

		// Costs
		long cost = original.getFinalCost();
		long costOnLine = onLine.getFinalCost();
		long costOffLine = offLine.getFinalCost();

		// Print the csv line entry
		ps.println(String.format(DATA_ROW_FORMAT, //
			absoluteExecutionTimeBasicPlanner, //
			absoluteExecutionTimeOnLinePlanner, relativeExecutionTimeOnLinePlanner, //
			absoluteExecutionTimeOffLinePlanner, relativeExecutionTimeOffLinePlanner, //
			//
			cost, costOnLine, costOffLine,
			//
			resourceUsage.getKey(), resourceUsage.getValue(), //
			resourceUsageOnLine.getKey(), resourceUsageOnLine.getValue(), //
			resourceUsageOffLine.getKey(), resourceUsageOffLine.getValue() //

		));
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}

	// Output a CSV file for each basic planner for FAST OSNAP
	for (String basicPlanner : basicPlanners) {
	    File outputFile = new File(String.format(FILE_NAME_FAST, basicPlanner));
	    outputFile.delete();

	    if (!offLinePlannersFAST.containsKey(basicPlanner))
		continue;

	    if (!onLinePlannersFAST.containsKey(basicPlanner))
		continue;

	    try (FileOutputStream fos = new FileOutputStream(outputFile); PrintStream ps = new PrintStream(fos);) {

		Schedule original = result.getSchedules().get(basicPlanner);
		Schedule onLine = result.getSchedules().get(onLinePlannersFAST.get(basicPlanner));
		Schedule offLine = result.getSchedules().get(offLinePlannersFAST.get(basicPlanner));

		// Absolute Time Planners
		long absoluteExecutionTimeBasicPlanner = result.getSchedules().get(basicPlanner).getFinalTime();
		//
		long absoluteExecutionTimeOnLinePlanner = onLine.getFinalTime();
		long absoluteExecutionTimeOffLinePlanner = offLine.getFinalTime();
		// Relative speedup/down
		double relativeExecutionTimeOnLinePlanner = ((double) (absoluteExecutionTimeBasicPlanner
			- absoluteExecutionTimeOnLinePlanner)) / absoluteExecutionTimeBasicPlanner * 100;
		double relativeExecutionTimeOffLinePlanner = ((double) (absoluteExecutionTimeBasicPlanner
			- absoluteExecutionTimeOffLinePlanner)) / absoluteExecutionTimeBasicPlanner * 100;

		// Resource usage
		Entry<Integer, Integer> resourceUsage = computeResourceUsage(original);
		Entry<Integer, Integer> resourceUsageOnLine = computeResourceUsage(onLine);
		Entry<Integer, Integer> resourceUsageOffLine = computeResourceUsage(offLine);

		// Costs
		long cost = original.getFinalCost();
		long costOnLine = onLine.getFinalCost();
		long costOffLine = offLine.getFinalCost();

		// Print the csv line entry
		ps.println(String.format(DATA_ROW_FORMAT, //
			absoluteExecutionTimeBasicPlanner, //
			absoluteExecutionTimeOnLinePlanner, relativeExecutionTimeOnLinePlanner, //
			absoluteExecutionTimeOffLinePlanner, relativeExecutionTimeOffLinePlanner, //
			//
			cost, costOnLine, costOffLine,
			//
			resourceUsage.getKey(), resourceUsage.getValue(), //
			resourceUsageOnLine.getKey(), resourceUsageOnLine.getValue(), //
			resourceUsageOffLine.getKey(), resourceUsageOffLine.getValue() //
		));
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

}
