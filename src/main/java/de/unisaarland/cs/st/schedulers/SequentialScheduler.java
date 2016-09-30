package de.unisaarland.cs.st.schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;

public class SequentialScheduler implements ITestJobScheduler {

    private Logger logger = Logger.getLogger(SequentialScheduler.class);

    /**
     * This assumes Jobs are ordered properly, so simply create a schedule by
     * reading them and updating start/end time !
     */
    @Override
    public Schedule solve(long startTime, Set<Job> jobs, CloudModel cloudModel, Goal goal) {

	// Adds up the times (setup+duration)
	List<Job> _jobs = new ArrayList<Job>();
	// Schedules start always from 0 ?!
	long finalTime = 0;
	long _startTime = 0;
	//
	for (Job cloudJob : jobs) {
	    if (!cloudJob.snapshot)
		continue;
	    cloudJob.startTime = _startTime;
	    // Note that processing time here identifies only setupTime !
	    cloudJob.endTime = cloudJob.startTime + cloudJob.processingTime;
	    if (cloudJob.processingTime < 0) {
		logger.error("Job " + ((cloudJob.snapshot) ? "SNAPSHOT" : "TESTJOB") + " " + cloudJob.image + " "
			+ cloudJob.parentImage + " " + cloudJob.testJob + " :: " + cloudJob.startTime + " --> "
			+ cloudJob.endTime);
	    }

	    _jobs.add(cloudJob);
	    // Update times
	    _startTime = finalTime = cloudJob.endTime;
	}
	//
	long snapshotDelay = finalTime;
	// This must be delayed no matter what !
	// Phase 2
	_startTime = snapshotDelay;
	logger.debug("Phase 2 start at: " + snapshotDelay);
	for (Job cloudJob : jobs) {
	    if (cloudJob.snapshot)
		continue;
	    cloudJob.startTime = _startTime;
	    // Note that processing time here identifies only setupTime !
	    cloudJob.endTime = cloudJob.startTime + cloudJob.processingTime;
	    if (cloudJob.processingTime < 0) {
		logger.error("Job " + ((cloudJob.snapshot) ? "SNAPSHOT" : "TESTJOB") + " " + cloudJob.image + " "
			+ cloudJob.parentImage + " " + cloudJob.testJob + " :: " + cloudJob.startTime + " --> "
			+ cloudJob.endTime);
	    }

	    _jobs.add(cloudJob);
	    // Update times
	    _startTime = finalTime = cloudJob.endTime;
	}
	// (int) (Math.ceil(((double) finalTime / (double) cloudModel.getBUT()))
	// * cloudModel.getCostOfReservedInstancePerBUT());

	Instance singleInstance = new Instance(1, true);
	Map<Instance, List<Job>> jobsDistribution = new HashMap<>();
	jobsDistribution.put(singleInstance, _jobs);

	//
	// long finalCost = Schedule.computeFinalCost(jobsDistribution,
	// cloudModel);
	// double objective = goal.computeObjective(finalTime, finalCost);
	long computationTime = System.currentTimeMillis() - startTime;

	return new Schedule(goal, cloudModel, jobsDistribution, computationTime);
    }

    @Override
    /**
     * This basically gives the plain scheduling time. ?! TODO Where is this
     * used ?
     * 
     * @param jobs
     * @param cloudModel
     * @param goal
     * @return
     */
    public Schedule solve(Set<Job> jobs, CloudModel cloudModel, Goal goal) {
	// Computation time --> scheduling time ?
	return solve(System.currentTimeMillis(), jobs, cloudModel, goal);
    }

}
