package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

public class StdOutSummaryScheduleProcessor implements IResultProcessor {

    @Override
    public void process(Result result) {
	PrintStream s = new PrintStream(System.out);

	Collections.sort(result.getPlannerNames());
	for (String plannerName : result.getPlannerNames()) {
	    Schedule schedule = result.getSchedules().get(plannerName);
	    s.println("Schedule Summary for: " + plannerName);
	    s.println("Obj " + schedule.objective);
	    s.println("Cost " + schedule.getFinalCost());
	    s.println("Time(sec) " + schedule.getFinalTime());
	    s.println("Comp_Time(sec) " + schedule.totalComputationTime);
	    // Count the number of total number of snapshots
	    int sCount = 0;
	    int sTestJob = 0;
	    for (List<Job> jobs : schedule.jobsDistribution.values()) {
		for (Job job : jobs) {
		    if (job.snapshot)
			sCount++;
		    else
			sTestJob++;
		}
	    }
	    s.println("N_Snapshots " + sCount);
	    s.println("N_TestJobs " + sTestJob);

	}
    }

}
