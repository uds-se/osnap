package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

public class StdOutListBasePlanners implements IResultProcessor {

    @Override
    public void process(Result result) {
	Set<String> basicPlanners = new HashSet<String>();
	// Basic planners name do not contain "With"
	for (String plannerName : result.getPlannerNames()) {
	    if (!plannerName.contains("With")) {
		basicPlanners.add(plannerName);
	    }
	}
	PrintStream ps = new PrintStream(System.out);
	for (String name : basicPlanners)
	    ps.println(name);
    }
}