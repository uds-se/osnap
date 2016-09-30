package de.unisaarland.cs.st.schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;

/**
 * This scheduler simply execute a job into a new instance assuming infinite
 * amount of instances. In fact, it just take the largest execution time as use
 * it as final time.
 * 
 * 
 * Assume only on-demand instances will be used
 * 
 * @author gambi
 *
 */
public class MaxParallelismScheduler implements ITestJobScheduler {

    private Logger logger = Logger.getLogger(MaxParallelismScheduler.class);

    private boolean onlyOnDemand = false;

    public MaxParallelismScheduler(boolean onlyOnDemand) {
	this.onlyOnDemand = onlyOnDemand;
    }

    public MaxParallelismScheduler() {
	this(false);
    }

    @Override
    public Schedule solve(long startTime, Set<Job> jobs, CloudModel cloudModel, Goal goal) {

	long finalTime = 0;
	int instanceID = 1;
	// This is basically the data structure behind the schedule
	Map<Instance, List<Job>> jobsDistribution = new HashMap<>();

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
	if (goal.maxOnDemandInstances < 0) {
	    goal.maxOnDemandInstances = jobs.size();
	}

	//
	List<Instance> reservedInstances = new ArrayList<Instance>(cloudModel.nReservedInstances);
	for (int i = 0; i < cloudModel.nReservedInstances; i++) {
	    reservedInstances.add(new Instance(i, true));
	}

	Iterator<Instance> reservedInstancesIterator = reservedInstances.iterator();

	// Phase 1 - Schedule Jobs with no precondition at phase 1.
	logger.debug("Phase 1 start at : " + 0);
	for (Job cloudJob : jobs) {

	    if (!cloudJob.snapshot && snapshotsToCreate.contains(cloudJob.image))
		continue;

	    logger.debug("Processing snapshot " + cloudJob.parentImage + " --> " + cloudJob.image);
	    // All the jobs start and the same time, zero for snapshots
	    cloudJob.startTime = 0;
	    //
	    cloudJob.endTime = cloudJob.startTime + cloudJob.processingTime;
	    //
	    if (cloudJob.processingTime < 0) {
		logger.error("Job " + ((cloudJob.snapshot) ? "SNAPSHOT" : "TESTJOB") + " " + cloudJob.image + " "
			+ cloudJob.parentImage + " " + cloudJob.testJob + " :: " + cloudJob.startTime + " --> "
			+ cloudJob.endTime);
	    }
	    // Jobs per instance
	    List<Job> _jobs = new ArrayList<Job>();
	    _jobs.add(cloudJob);

	    Instance _theInstance = null;
	    if (reservedInstancesIterator.hasNext() && !onlyOnDemand) {
		_theInstance = reservedInstancesIterator.next();
	    } else {
		Instance freshInstance = new Instance(instanceID, false);
		_theInstance = freshInstance;
	    }

	    jobsDistribution.put(_theInstance, _jobs);

	    // Update times
	    finalTime = Math.max(cloudJob.endTime, finalTime);
	    // Increment instance id
	    instanceID++;
	    //
	    // finalCost = finalCost + (int) (Math.ceil(((double)
	    // cloudJob.processingTime / (double) cloudModel.getBUT()))
	    // * cloudModel.getCostOfOnDemandInstancePerBUT());

	}
	//
	// TODO This can be improved by computing the ending time at each
	// snapshot, not simply taking the biggest one by default

	// Compute the initial delay here
	long snapshotDelay = finalTime;
	// This must be delayed no matter what !
	// Phase 2
	logger.debug("Phase 2 start at: " + snapshotDelay);
	for (Job cloudJob : jobs) {
	    //
	    if (cloudJob.snapshot || !snapshotsToCreate.contains(cloudJob.image))
		continue;
	    //

	    logger.debug("Processing test job " + cloudJob.testJob);
	    // All the jobs start and the same time
	    cloudJob.startTime = snapshotDelay;
	    // Note that processing time here identifies only setupTime !
	    cloudJob.endTime = cloudJob.startTime + cloudJob.processingTime;
	    //
	    if (cloudJob.processingTime < 0) {
		logger.error("Job " + ((cloudJob.snapshot) ? "SNAPSHOT" : "TESTJOB") + " " + cloudJob.image + " "
			+ cloudJob.parentImage + " " + cloudJob.testJob + " :: " + cloudJob.startTime + " --> "
			+ cloudJob.endTime);
	    }
	    // Jobs per instance
	    List<Job> _jobs = new ArrayList<Job>();
	    _jobs.add(cloudJob);

	    // Use only on demand ? Here make little difference !
	    Instance _theInstance = null;
	    if (reservedInstancesIterator.hasNext() && !onlyOnDemand) {
		_theInstance = reservedInstancesIterator.next();
	    } else {
		Instance freshInstance = new Instance(instanceID, false);
		_theInstance = freshInstance;
	    }
	    jobsDistribution.put(_theInstance, _jobs);

	    // Update times
	    finalTime = Math.max(cloudJob.endTime, finalTime);
	    // Increment instance id
	    instanceID++;
	    //
	    // finalCost = finalCost + (int) (Math.ceil(((double)
	    // cloudJob.processingTime / (double) cloudModel.getBUT()))
	    // * cloudModel.getCostOfOnDemandInstancePerBUT());
	}

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
