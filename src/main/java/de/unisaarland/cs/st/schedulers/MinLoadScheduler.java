package de.unisaarland.cs.st.schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;

/**
 * This scheduler sends a new job into the available instance which has the
 * smallest execution time so far
 * 
 * Assume N reserved instances are available. N=1 this scheduler behaves like
 * SequentialScheduler.
 * 
 * @author gambi
 *
 */
public class MinLoadScheduler implements ITestJobScheduler {

    private Logger logger = Logger.getLogger(MinLoadScheduler.class);

    private boolean containsTestJobs(List<Job> jobs) {
	for (Job j : jobs) {
	    if (!j.snapshot)
		return true;
	}
	return false;
    }

    // Assume N>=1 Instances !
    private Instance selectInstanceWithMinExecutionTime(Map<Instance, List<Job>> jobsDistribution) {
	long minExecutionTime = Long.MAX_VALUE;
	Instance instance = null;

	for (Entry<Instance, List<Job>> jobsEntry : jobsDistribution.entrySet()) {
	    List<Job> _jobs = jobsEntry.getValue();
	    long instanceExecutionTime = (_jobs.isEmpty()) ? 0 : _jobs.get(_jobs.size() - 1).endTime;
	    if (instanceExecutionTime == minExecutionTime)
		/*
		 * Same min, just got !
		 */
		continue;
	    //
	    minExecutionTime = Math.min(minExecutionTime, instanceExecutionTime);
	    // At this point if they are the same, then instanceExecution time
	    // is actually the min time.
	    if (instanceExecutionTime == minExecutionTime)
		instance = jobsEntry.getKey();
	}

	// System.out.println("MinLoadScheduler.selectInstanceWithMinExecutionTime()
	// " + instance);
	return instance;
    }

    /**
     * In the presence of snapshots and dependent tests adopt the following
     * heuristic: - Schedule first snapshots and test jobs which do not use any
     * snapshot - Parallelize using the current strategy, i.e., min load) -
     * Compute the maximum time (phase 1) - The schedule all the other test jobs
     * which depends on the snapshots
     */
    @Override
    public Schedule solve(long startTime, Set<Job> jobs, CloudModel cloudModel, Goal goal) {

	// This is basically the data structure behind the schedule
	Map<Instance, List<Job>> jobsDistribution = new HashMap<>();
	// I though this was already fixed !
	Instance[] reservedInstances = new Instance[cloudModel.nReservedInstances];
	for (int i = 0; i < reservedInstances.length; i++) {
	    reservedInstances[i] = new Instance(i, true);
	    //
	    jobsDistribution.put(reservedInstances[i], new ArrayList<Job>());
	}

	int instanceId = 0;
	long finalTime = 0;

	// Snapshot set !
	Set<Image> snapshotsToCreate = new HashSet<Image>();
	for (Job cloudJob : jobs) {
	    if (!cloudJob.snapshot) {
		continue;
	    } else {
		snapshotsToCreate.add(cloudJob.image);
	    }
	}
	//
	logger.debug("Snapshots to be created " + snapshotsToCreate);

	// Allocate jobs which have not deps first !
	for (Job cloudJob : jobs) {
	    // Either this is a snapshot or a test which does not depends on
	    // snapshots
	    if (!cloudJob.snapshot && snapshotsToCreate.contains(cloudJob.image)) {
		logger.debug("Phase 1: skipping " + cloudJob);
		logger.debug(cloudJob.snapshot);
		logger.debug(cloudJob.image);
		logger.debug(cloudJob.processingTime);
		continue;
	    } else {
		logger.debug("Phase 1: allocating " + cloudJob);
		logger.debug(cloudJob.snapshot);
		logger.debug(cloudJob.image);
		logger.debug(cloudJob.processingTime);
	    }

	    // Select instance's already allocated jobs
	    // List<Job> _jobs =
	    // jobsDistribution.get(reservedInstances[instanceId]);

	    // Select instance's already allocated jobs -- Use the current Min
	    // Load policy also here !
	    List<Job> _jobs = jobsDistribution.get(selectInstanceWithMinExecutionTime(jobsDistribution));
	    // This job start when the last one in this instance finishes, if
	    // any
	    cloudJob.startTime = (_jobs.isEmpty()) ? 0 : _jobs.get(_jobs.size() - 1).endTime;
	    // Note that processing time here identifies only setupTime !
	    cloudJob.endTime = cloudJob.startTime + cloudJob.processingTime;
	    //
	    if (cloudJob.processingTime < 0) {
		logger.error("Job " + ((cloudJob.snapshot) ? "SNAPSHOT" : "TESTJOB") + " " + cloudJob.image + " "
			+ cloudJob.parentImage + " " + cloudJob.testJob + " :: " + cloudJob.startTime + " --> "
			+ cloudJob.endTime);
	    }
	    // Update the instance's jobs
	    _jobs.add(cloudJob);
	    // Update indices
	    instanceId = (instanceId + 1) % reservedInstances.length;
	}

	for (List<Job> jobsPerInstance : jobsDistribution.values()) {
	    finalTime = Math.max(finalTime,
		    (jobsPerInstance.isEmpty()) ? 0 : jobsPerInstance.get(jobsPerInstance.size() - 1).endTime);
	}
	//
	long snapshotDelay = finalTime;
	// This must be delayed no matter what !
	// Phase 2
	logger.debug("Phase 2 start at: " + snapshotDelay);

	for (Job cloudJob : jobs) {

	    if (cloudJob.snapshot || !snapshotsToCreate.contains(cloudJob.image))
		continue;

	    // Select instance's already allocated jobs
	    List<Job> _jobs = jobsDistribution.get(selectInstanceWithMinExecutionTime(jobsDistribution));
	    // This job start when the last one in this instance finishes, if
	    // any
	    cloudJob.startTime = (_jobs.isEmpty() || !containsTestJobs(_jobs)) ? snapshotDelay
		    : _jobs.get(_jobs.size() - 1).endTime;
	    //
	    // We muse ensure that we wait for all the snapshots to complete
	    if (cloudJob.startTime < snapshotDelay)
		cloudJob.startTime = snapshotDelay;

	    // Note that processing time here identifies only setupTime !
	    cloudJob.endTime = cloudJob.startTime + cloudJob.processingTime;
	    //
	    if (cloudJob.processingTime < 0) {
		logger.error("Job " + ((cloudJob.snapshot) ? "SNAPSHOT" : "TESTJOB") + " " + cloudJob.image + " "
			+ cloudJob.parentImage + " " + cloudJob.testJob + " :: " + cloudJob.startTime + " --> "
			+ cloudJob.endTime);
	    }
	    // Update the instance's jobs
	    _jobs.add(cloudJob);
	}

	// Compute the makespan/finaltime as the max of all the execution times
	// for (List<Job> jobsPerInstance : jobsDistribution.values()) {
	// finalTime = Math.max(finalTime,
	// (jobsPerInstance.isEmpty()) ? 0 :
	// jobsPerInstance.get(jobsPerInstance.size() - 1).endTime);
	// }
	// long finalCost = Schedule.computeFinalCost(jobsDistribution,
	// cloudModel);
	// double objective = goal.computeObjective(finalTime, finalCost);

	long computationTime = System.currentTimeMillis() - startTime;

	return new Schedule(goal, cloudModel, jobsDistribution, computationTime);
    }

    @Override
    public Schedule solve(Set<Job> jobs, CloudModel cloudModel, Goal goal) {
	return solve(System.currentTimeMillis(), jobs, cloudModel, goal);
    }

}
