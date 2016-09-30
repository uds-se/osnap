package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.File;
import java.io.FileOutputStream;
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

public class PackageTiming implements IResultProcessor {

    // private final String DATA_ROW_FORMAT = "%-30s %-10s\t%5s\t%5s\t%5s";
    private final String DATA_ROW_FORMAT = "%s,%s,%s,%s"; // CSV Output
    private final String FILE_NAME = "%s.package-timining.csv";

    @Override
    public void process(Result result) {
	for (Entry<String, Schedule> entry : result.getSchedules().entrySet()) {
	    File outputFile = new File(String.format(FILE_NAME, entry.getKey()));
	    outputFile.delete();

	    try (FileOutputStream fos = new FileOutputStream(outputFile); PrintStream ps = new PrintStream(fos);) {

		Schedule schedule = entry.getValue();
		// Extract the TestJobs Merge the jobs together, we do not
		// really
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

		// s.println(nEntries);
		// Now print for each test job the time with empty, base-image
		// and
		// with the given image/snapshot
		for (TestJob testJob : testJobs) {
		    ps.println(String.format(DATA_ROW_FORMAT, //
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
		// ps.println();
	    } catch (Exception e) {
		// Skip this continue with next one
		e.printStackTrace();
	    }
	}
    }

}
