package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.PrintStream;

import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;
import de.unisaarland.cs.st.planners.MaxParallelismPlannerOnlyOnDemand;
import de.unisaarland.cs.st.planners.SequentialPlanner;

public class NormalizingFactors implements IResultProcessor {

    // This goes only with Max and Sequential
    @Override
    public void process(Result result) {

	try (PrintStream s = new PrintStream(System.out);) {

	    long alphaN = -1; // Max Cost means, final cost of MaxParallelism
	    long betaN = -1; // Max Time means, final time of Sequential

	    for (String plannerName : result.getPlannerNames()) {
		if (plannerName.equals(MaxParallelismPlannerOnlyOnDemand.NAME)) {
		    alphaN = result.getSchedules().get(plannerName).getFinalCost();
		} else if (plannerName.equals(SequentialPlanner.NAME)) {
		    betaN = result.getSchedules().get(plannerName).getFinalTime();
		} else {
		    // Ignored
		}
	    }
	    // Not sure about \n
	    s.println(String.format("%s %s\n", alphaN, betaN));
	}
    }

}
