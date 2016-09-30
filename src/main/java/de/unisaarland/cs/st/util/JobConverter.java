package de.unisaarland.cs.st.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import de.unisaarland.cs.st.data.TestJob;

public class JobConverter {

    private static final Logger logger = Logger.getLogger(JobConverter.class);
    // public static final String OFFLINE_MODE = "offline";

    public static Set<Job> toJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> partialSolution,
	    CloudModel cloudModel) {
	return toJobs(partialSolution, cloudModel, true);
    }

    public static Set<Job> toJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> partialSolution,
	    CloudModel cloudModel, boolean online) {

	// Default is online !
	// boolean online = System.getProperty(OFFLINE_MODE) == null;

	Set<Job> jobs = new HashSet<Job>();
	if (online) {
	    logger.info("ON LINE Mode Active: We include snapshot creation in the processing time !");

	    // Parent, Snapshot
	    for (Entry<Image, Image> snapshot : partialSolution.getValue()) {
		// Processing time = setupTime
		long processingTime = snapshot.getValue().getSetupTimeWithImage(snapshot.getKey())
			+ cloudModel.getTimeToSnapshot();
		//
		Job job = Job.createNewJob(processingTime);
		//

		logger.debug(" Processing Snapshot " + snapshot.getKey() + " ==> " + snapshot.getValue() + " takes "
			+ processingTime);

		job.snapshot = true;
		job.parentImage = snapshot.getKey();
		job.image = snapshot.getValue();
		//
		jobs.add(job);
	    }
	} else {
	    logger.debug("OFF LINE Mode active: We do not include snapshot creation in the scheduling !!");
	}
	for (Entry<TestJob, Image> test : partialSolution.getKey().entrySet()) {
	    // Processing time = setupTime + testDuration
	    long processingTime = test.getKey().getSetupTimeWithImage(test.getValue()) + test.getKey().testDuration;
	    //
	    Job job = Job.createNewJob(processingTime);
	    job.testJob = test.getKey();
	    job.image = test.getValue();
	    // Lookup for deps
	    for (Job j : jobs) {
		if (j.snapshot && j.image.equals(test.getValue())) {
		    job.dependsOn.add(j);
		    break;
		}
	    }
	    //
	    jobs.add(job);
	}

	return jobs;
    }

    // By default on-line
    public static Set<Job> toOderedJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> partialSolution,
	    CloudModel cloudModel) {
	return toOderedJobs(partialSolution, cloudModel, true);
    }

    public static Set<Job> toOderedJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> partialSolution,
	    CloudModel cloudModel, boolean online) {
	List<Job> list = toOderedJobsAsList(partialSolution, cloudModel, online);
	Set<Job> orderedSet = new LinkedHashSet<Job>();
	for (Job j : list) {
	    orderedSet.add(j);
	}
	return orderedSet;
    }

    /**
     * Return the set of job to be scheduled ordered as snapshots first and test
     * later, this way we can ensure that def/use relations are respected. But
     * this does not set start or end time for jobs, only their duration and
     * order !
     * 
     * @param mapping
     * @return
     */
    public static List<Job> toOderedJobsAsList(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> partialSolution,
	    CloudModel cloudModel) {
	return toOderedJobsAsList(partialSolution, cloudModel, true);
    }

    public static List<Job> toOderedJobsAsList(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> partialSolution,
	    CloudModel cloudModel, boolean online) {

	// Default is online !
	// boolean online = System.getProperty(OFFLINE_MODE) == null;

	List<Job> jobs = new ArrayList<Job>();
	if (online) {
	    logger.info("ON LINE Mode ACTIVE: We include snapshot creation in the processing time !");
	    // Parent, Snapshot
	    for (Entry<Image, Image> snapshot : partialSolution.getValue()) {
		// Processing time = setupTime
		long processingTime = snapshot.getValue().getSetupTimeWithImage(snapshot.getKey())
			+ cloudModel.getTimeToSnapshot();
		//
		Job job = Job.createNewJob(processingTime);
		//
		logger.debug(" Processing Snapshot " + snapshot.getKey().name + " ==> " + snapshot.getValue().name
			+ " takes " + processingTime);

		job.snapshot = true;
		job.parentImage = snapshot.getKey();
		job.image = snapshot.getValue();
		//
		jobs.add(job);
	    }
	} else {
	    logger.info("OFF LINE Mode ACTIVE!");
	}

	// Parent, Snapshot
	// for (Entry<Image, Image> snapshot : mapping.getValue()) {
	// // Processing time = setupTime
	// long processingTime =
	// snapshot.getValue().getSetupTimeWithImage(snapshot.getKey())
	// + cloudModel.getTimeToSnapshot();
	// //
	// Job job = Job.createNewJob(processingTime);
	// job.snapshot = true;
	// job.parentImage = snapshot.getKey();
	// job.image = snapshot.getValue();
	// //
	// jobs.add(job);
	// }

	for (Entry<TestJob, Image> test : partialSolution.getKey().entrySet()) {
	    // Processing time = setupTime + testDuration
	    long processingTime = test.getKey().getSetupTimeWithImage(test.getValue()) + test.getKey().testDuration;

	    if (processingTime < 0) {
		logger.error("Processing time for test job " + test.getKey() + "  is negative = " + processingTime);
		logger.error("Setup time " + test.getKey().getSetupTimeWithImage(test.getValue()));
		logger.error("Setup time " + test.getKey().getSetupTimeWithImageMillisec(test.getValue()));
		logger.error("Setup time " + Double.MAX_VALUE + " " + Long.MAX_VALUE + " " + Integer.MAX_VALUE);
		logger.error("Test Duration" + test.getKey().testDuration);

	    }
	    //
	    Job job = Job.createNewJob(processingTime);
	    job.testJob = test.getKey();
	    job.image = test.getValue();
	    // Lookup for deps
	    for (Job j : jobs) {
		if (j.snapshot && j.image.equals(test.getValue())) {
		    job.dependsOn.add(j);
		    break;
		}
	    }
	    //
	    jobs.add(job);
	}

	return jobs;
    }

    // Keep the ordering of the initial schedule plus the one imposed by the
    // snapshots !
    public static Set<Job> toJobsFromSchedule(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> partialSolution,
	    Schedule schedule, CloudModel cloudModel, Goal goal) {
	// Reuse the Jobs and PrecedenceRelation
	Set<Job> jobs = new HashSet<Job>();

	// Within the same instance jobs must maintain their original order

	System.out.println("JobConverter.toJobsFromSchedule() Adding ordering data for "
		+ schedule.jobsDistribution.entrySet().size());
	for (Entry<Instance, List<Job>> scheduleEntry : schedule.jobsDistribution.entrySet()) {
	    // Instance instance = scheduleEntry.getKey();
	    List<Job> scheduledJobs = scheduleEntry.getValue();
	    System.out.println("JobConverter.toJobsFromSchedule() Scheduled Jobs " + scheduledJobs.size());
	    for (int i = 0; i < scheduledJobs.size(); i++) {
		for (int j = 0; j > i && j < scheduledJobs.size(); j++) {
		    System.out.println("JobConverter.toJobsFromSchedule() Processing ");
		    // if (i < j) {
		    scheduledJobs.get(j).dependsOn.add(scheduledJobs.get(i));
		    // }
		}
	    }
	    jobs.addAll(scheduledJobs);
	}

	// Introduce Snapshots (not there before!)
	logger.trace("JobConverter.toJobsFromSchedule() New Snapshot constraints");
	System.out.println("JobConverter.toJobsFromSchedule() Adding snapshot contraints for "
		+ partialSolution.getValue().size());
	for (Entry<Image, Image> snapshot : partialSolution.getValue()) {
	    // Processing time = setupTime
	    long processingTime = snapshot.getValue().getSetupTimeWithImage(snapshot.getKey())
		    + cloudModel.getTimeToSnapshot();
	    //
	    Job job = Job.createNewJob(processingTime);
	    logger.trace("JobConverter.toJobsFromSchedule() Snapshot job is " + job.id);
	    job.snapshot = true;
	    job.parentImage = snapshot.getKey();
	    job.image = snapshot.getValue();
	    //
	    jobs.add(job);
	}

	// Update TestJobs image but not the time(s) for that we need the
	// scheduler.
	// if they are allocated to new image, then they must be updated to
	// reflect new setup time !

	System.out.println("JobConverter.toJobsFromSchedule() Updating original scheduler for "
		+ partialSolution.getKey().entrySet().size());
	for (Entry<TestJob, Image> test : partialSolution.getKey().entrySet()) {
	    // Lookup the original job. If we find a job corresponding to the
	    // same testJob but with different Image (snapshot !) then we must
	    // update its data
	    for (Job job : jobs) {

		if (!job.snapshot && job.testJob.equals(test.getKey()) && !job.image.equals(test.getValue())) {

		    // Not sure this is safe
		    long delta = (job.processingTime
			    - (test.getKey().getSetupTimeWithImage(test.getValue()) + test.getKey().testDuration));

		    logger.trace("Test Job " + job.testJob.id + "(" + job.id + ") was udpated: " + job.image + " --> "
			    + test.getValue() + "; duration :  " + job.processingTime + " --> "
			    + (test.getKey().getSetupTimeWithImage(test.getValue()) + test.getKey().testDuration)
			    + " (delta is " + delta + ")");
		    //
		    job.image = test.getValue();
		    job.processingTime = test.getKey().getSetupTimeWithImage(test.getValue())
			    + test.getKey().testDuration;
		    job.delta = delta;

		    // Since we introduced a new snapshot we must update the
		    // deps of this job
		    for (Job j : jobs) {
			if (j.snapshot && j.image.equals(test.getValue())) {
			    job.dependsOn.add(j);
			    break;
			}
		    }
		}
	    }
	}

	// Update starting and ending time of the jobs - Processing time now is
	// the new value !
	for (List<Job> scheduledJobsOnInstance : schedule.jobsDistribution.values()) {

	    // Possibly no test jobs allocated to this instance
	    if (scheduledJobsOnInstance.isEmpty())
		continue;

	    // Update the first
	    scheduledJobsOnInstance.get(0).endTime = scheduledJobsOnInstance.get(0).startTime
		    + scheduledJobsOnInstance.get(0).processingTime;
	    for (int i = 1; i < scheduledJobsOnInstance.size(); i++) {
		scheduledJobsOnInstance.get(i).startTime = scheduledJobsOnInstance.get(i - 1).endTime;
		scheduledJobsOnInstance.get(i).endTime = scheduledJobsOnInstance.get(i).startTime
			+ scheduledJobsOnInstance.get(i).processingTime;
	    }
	}

	// Update costs and time
	long total_time = Long.MIN_VALUE;
	for (List<Job> scheduledJobsOnInstance : schedule.jobsDistribution.values()) {
	    // It might happen that for reserved instances we have not job to
	    // run, therefore this list might be empty
	    if (scheduledJobsOnInstance.size() == 0)
		continue;

	    //
	    total_time = Long.max(total_time, scheduledJobsOnInstance.get(scheduledJobsOnInstance.size() - 1).endTime);
	}
	schedule.setFinalTime(total_time);
	// Costs
	int total_cost = 0;
	for (Entry<Instance, List<Job>> scheduleEntry : schedule.jobsDistribution.entrySet()) {

	    if (scheduleEntry.getValue().size() == 0)
		continue;

	    int costPerBUT = (scheduleEntry.getKey().isReserved()) ? cloudModel.getCostOfReservedInstancePerBUT()
		    : cloudModel.getCostOfOnDemandInstancePerBUT();

	    long usageTime = scheduleEntry.getValue().get(scheduleEntry.getValue().size() - 1).endTime;
	    total_cost = total_cost + costPerBUT * (int) Math.ceil((double) usageTime / (double) cloudModel.but);
	}
	schedule.setFinalCost(total_cost);
	schedule.objective = goal.computeObjective(schedule.getFinalTime(), schedule.getFinalCost());
	return jobs;
    }

    /**
     * Update the jobs by translating them in time by offSet
     * 
     * @param scheduledJobs
     * @param offSet
     */
    public static void translateBy(List<Job> scheduledJobs, long offSet) {
	for (Job job : scheduledJobs) {
	    job.startTime = job.startTime + offSet;
	    job.endTime = job.endTime + offSet;
	}
    }

}
