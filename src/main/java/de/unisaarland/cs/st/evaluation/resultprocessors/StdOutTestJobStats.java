package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;
import de.unisaarland.cs.st.data.Package;

/**
 * Prints on stdout some stats about the input files that lead to this result
 * 
 * @author gambi
 *
 */
public class StdOutTestJobStats implements IResultProcessor {

    @Override
    public void process(Result result) {
	// Assume all the results have the same test-input
	Set<TestJob> testJobs = new HashSet<TestJob>();
	Set<Package> uniqueDeps = new HashSet<Package>();
	Set<Package> sharedDeps = new HashSet<Package>();

	boolean init = false;
	for (Entry<String, Schedule> entry : result.getSchedules().entrySet()) {
	    for (List<Job> jobs : entry.getValue().jobsDistribution.values()) {
		for (Job job : jobs) {
		    if (!job.snapshot) {
			testJobs.add(job.testJob);
			// Union
			uniqueDeps.addAll(job.testJob.sut.dependencies);
			// Intersection - Shared
			if (!init) {
			    sharedDeps.addAll(job.testJob.sut.dependencies);
			    init = true;
			} else {
			    sharedDeps.retainAll(job.testJob.sut.dependencies);
			}
		    }
		}
	    }
	}

	PrintStream ps = new PrintStream(System.out);
	ps.println("Sample size: " + testJobs.size());
	ps.println("Unique Deps: " + uniqueDeps.size());
	ps.println("Shared Deps: " + sharedDeps.size());
	ps.println("Test Jobs: " + testJobs.size());

    }
}