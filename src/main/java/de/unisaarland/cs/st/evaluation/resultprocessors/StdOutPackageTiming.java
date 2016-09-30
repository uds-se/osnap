package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

public class StdOutPackageTiming implements IResultProcessor {

    // private final String DATA_ROW_FORMAT = "%-30s %-10s\t%5s\t%5s\t%5s";
    private final String DATA_ROW_FORMAT = "%s,%s,%s,%s"; // CSV Output

    @Override
    public void process(Result result) {
	// TODO Make this configurable ?
	PrintStream s = new PrintStream(System.out);

	for (Entry<String, Schedule> entry : result.getSchedules().entrySet()) {
	    Schedule schedule = entry.getValue();
	    // Extract the TestJobs Merge the jobs together, we do not really
	    // care about Instances right now
	    // For each Package in each Test Job
	    Set<TestJob> testJobs = new HashSet<TestJob>();
	    for (List<Job> jobsPerInstance : schedule.jobsDistribution.values()) {
		for (Job job : jobsPerInstance) {
		    if (!job.snapshot) { // Take only test jobs
			testJobs.add(job.testJob);
		    }
		}
	    }
	    s.println("Package Timing Data for: " + entry.getKey());
	    // s.println(nEntries);
	    // Now print for each test job the time with empty, base-image and
	    // with the given image/snapshot
	    for (TestJob testJob : testJobs) {
		s.println(String.format(DATA_ROW_FORMAT, //
			testJob.sut.name + "_" + testJob.sut.version, //
			testJob.getSetupTimeWithImage(Image.getEmptyImage()), //
			testJob.getSetupTimeWithImage(schedule.baseImage), //
			testJob.getSetupTimeWithImage(testJob.runWith)//
		));

		// System.out.println("StdOutPackageTiming.process() " +
		// Image.getEmptyImage() );
		// System.out.println("StdOutPackageTiming.process() " +
		// schedule.baseImage );
		// System.out.println("StdOutPackageTiming.process() " +
		// testJob.runWith );
	    }
	    // Empty line defines the next result
	    s.println();
	}
    }

}
