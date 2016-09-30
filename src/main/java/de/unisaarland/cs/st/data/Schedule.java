package de.unisaarland.cs.st.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class Schedule implements Serializable {

    // Will this break the code?
    private static final Logger logger = Logger.getLogger(Schedule.class);
    /**
     * 
     */
    private static final long serialVersionUID = 2060865044651175946L;
    //
    public double objective;
    ///
    private long finalCost;
    private long finalTime;

    public long getFinalCost() {
	return finalCost;
    }

    public long getFinalTime() {
	return finalTime;
    }

    // TODO Those might be removed soon
    @Deprecated
    public void setFinalCost(long finalCost) {
	this.finalCost = finalCost;
    }

    @Deprecated
    public void setFinalTime(long finalTime) {
	this.finalTime = finalTime;
    }

    // This might be a problem !
    //
    public Map<Instance, List<Job>> jobsDistribution = new HashMap<Instance, List<Job>>();

    // Keep this sec
    public double totalComputationTime = -1;
    public double problemCreationTime = -1;
    public double problemSolutionTime = -1;
    //
    public Image baseImage;

    public Schedule() {
	// TODO Auto-generated constructor stub
    }

    /**
     * Note that this will update the utilization value of reserved instances !
     * 
     * @param jobsDistribution
     */
    public static void computeUtilizationOfReservedInstances(Map<Instance, List<Job>> jobsDistribution) {
	long finalTime = Schedule.computeFinalTime(jobsDistribution);
	// Avoid corner cases !
	if (finalTime == 0) {
	    for (Instance i : jobsDistribution.keySet()) {
		i.utilization = Double.NaN;
	    }
	} else {
	    for (Entry<Instance, List<Job>> e : jobsDistribution.entrySet()) {
		Instance i = e.getKey();
		List<Job> jobs = e.getValue();
		long partialTime = (jobs.isEmpty()) ? 0 : jobs.get(jobs.size() - 1).endTime;
		i.utilization = ((double) partialTime) / finalTime * 100;
	    }
	}
    }

    // Assume that Jobs are ordered !
    public static long computeFinalTime(Map<Instance, List<Job>> jobsDistribution) {
	// Iterate over the jobs and take the biggest one !
	long finalTime = 0;
	for (List<Job> jobs : jobsDistribution.values()) {
	    finalTime = Math.max(finalTime, (jobs.isEmpty()) ? 0 : jobs.get(jobs.size() - 1).endTime);
	}
	return finalTime;
    }

    public static long computeFinalCost(Map<Instance, List<Job>> jobsDistribution, CloudModel cloudModel) {
	long costForReserved = 0;
	long costForOnDemand = 0;
	for (Entry<Instance, List<Job>> e : jobsDistribution.entrySet()) {
	    long usageOfInstance = 0;
	    for (Job j : e.getValue()) {
		usageOfInstance = usageOfInstance + j.processingTime;
	    }
	    if (e.getKey().isReserved()) {
		/*
		 * Let's assume that reserved have a all or nothing. If they are
		 * not used we do not have to pay, otherwise we pay a fixed
		 * amount which is constant across the execution. Ideally the
		 * metric would be utilization, i.e., amount of time each
		 * reserved is used. The more the better.
		 */
		costForReserved = costForReserved
			+ ((usageOfInstance > 0) ? cloudModel.getCostOfReservedInstance() : 0);
	    } else /*
		    * OnDemand : Cost of instance if greatest/next integer in
		    * time give the billing unit times the price per billing
		    * unit
		    */
		costForOnDemand = costForOnDemand + (long) Math.ceil(((double) usageOfInstance) / cloudModel.getBUT())
			* cloudModel.getCostOfOnDemandInstancePerBUT();
	}
	logger.trace("costForReserved: " + costForReserved);
	logger.trace("costForOnDemand: " + costForOnDemand);
	return (costForReserved + costForOnDemand);

    }

    /**
     * Compute the objective, finalTime and finalCost on the fly !
     * 
     * @param goal
     * @param cloudModel
     * @param jobsDistribution
     * @param computationTime
     */
    public Schedule(Goal goal, CloudModel cloudModel, Map<Instance, List<Job>> jobsDistribution, long computationTime) {

	this.finalCost = Schedule.computeFinalCost(jobsDistribution, cloudModel);
	this.finalTime = Schedule.computeFinalTime(jobsDistribution);
	this.objective = goal.computeObjective(this.finalTime, this.finalCost);
	this.jobsDistribution = jobsDistribution;
	// Make it seconds
	this.totalComputationTime = ((double) computationTime) / 1000;
    }

    // TODO Final Cost and Final Time are derived entities ! To have a schedule
    // just jobsDistribution, CloudModel and Gaol might be necessary
    // Not even those !
    private Schedule(double objective, long finalCost, long finalTime, Map<Instance, List<Job>> jobsDistribution,
	    // This is millisec
	    long computationTime) {
	super();
	this.objective = objective;
	this.finalCost = finalCost;
	this.finalTime = finalTime;
	this.jobsDistribution = jobsDistribution;
	// Make it seconds
	this.totalComputationTime = ((double) computationTime) / 1000;
    }

    public String toString() {
	StringBuffer sb = new StringBuffer();
	sb.append("------------------------------------------------------------\n");
	sb.append(String.format("%-20s %10s\n", "Objective:", objective));
	sb.append("------------------------------------------------------------\n");
	sb.append(String.format("%-20s %10s %s\n", "  Time: ", finalTime, "sec"));
	sb.append(String.format("%-20s %10s %s\n", "  Cost:", finalCost, "$"));
	sb.append("------------------------------------------------------------\n");
	sb.append(String.format("%-20s %10s %s\n", "Computed in:", (totalComputationTime / 1000), "sec"));
	sb.append("------------------------------------------------------------\n");

	List<Entry<Instance, List<Job>>> orderedEntries = new ArrayList<Entry<Instance, List<Job>>>(
		jobsDistribution.entrySet());
	Collections.sort((List<Entry<Instance, List<Job>>>) orderedEntries,
		new Comparator<Entry<Instance, List<Job>>>() {

		    @Override
		    public int compare(Entry<Instance, List<Job>> o1, Entry<Instance, List<Job>> o2) {
			// Reserved first, On demand later
			if (o1.getKey().isReserved() && !o2.getKey().isReserved()) {
			    return -1;
			} else if (!o1.getKey().isReserved() && o2.getKey().isReserved()) {
			    return 1;
			} else {
			    // For the same type. id decides
			    return o1.getKey().getId() - o2.getKey().getId();
			}
		    }

		});
	for (Entry<Instance, List<Job>> instance : orderedEntries) {

	    if (!instance.getValue().isEmpty()) {
		if (instance.getKey().isReserved()) {
		    sb.append("------------------- RESERVED INSTANCE " + instance.getKey().getId()
			    + " --------------------\n");
		} else {
		    sb.append("------------------- ON-DEMAND INSTANCE " + instance.getKey().getId()
			    + " --------------------\n");
		}
		// Print jobs
		for (Job j : instance.getValue()) {

		    sb.append(String.format("%4s %-4s %-40s %5s -> %5s ", //
			    j.id, (j.snapshot) ? "S" : "T",
			    (j.snapshot) ? j.parentImage.getId() + "->" + j.image.getId()
				    : j.testJob.sut + " " + new DateTime(j.testJob.submissionTime),

			    j.startTime, j.endTime));

		    sb.append(String.format("%4s", (j.snapshot) ? j.parentImage.getId() : j.image.getId()));

		    if (j.snapshot) {
			sb.append(String.format(" %-3s: %5s -> %5s", "PAR", j.parentImage, j.image));
		    }
		    // else {
		    // sb.append(String.format(" %-3s: %5s", "TID",
		    // j.testJob.id));
		    // }
		    sb.append("\n");

		    // sb.append("\t" + ((j.snapshot)
		    // ? j.id + " SNAPSHOT " + ((j.parentImage != null) ?
		    // j.parentImage.getLabel() : "null")
		    // + " --> " + j.image.getLabel()
		    // : j.id + " TEST JOB " + j.testJob.id))
		    // .append(String.format("\t%d -> %d\t%s\n", j.startTime,
		    // j.endTime,
		    // (j.image != null) ? j.image.getLabel() : "null"));
		}
		sb.append("------------------------------------------------------------\n");
	    }
	}

	return sb.toString();
    }

    // public static final Schedule EMPTY_SCHEDULE = new Schedule(0, 0, 0,
    // Collections.EMPTY_MAP, 0);

    public static Schedule newEmptySchedule() {
	return new Schedule(0, 0, 0, Collections.EMPTY_MAP, 0);
    }

    public static final Schedule ERROR_SCHEDULE = new Schedule(-1, -1, -1, Collections.EMPTY_MAP, 0);

    // Merge two schedules, basically put the second after the first, reuse
    // reserved instances, add on=demand instances
    // Note that this is NOT EASY AT ALL !!!
    // NOTE That schedule must be compatible, e.g. no more reosurces than needed
    // and such. We do not check nor enforce those things !!
    // FIXME CLone or deep copy is better At the moment both objects are
    // modified !!!
    public void mergeWith(Schedule schedule) {

	if (schedule.equals(Schedule.ERROR_SCHEDULE)) {
	    // TODO - This shall become error ? Or we throw an exception ?
	    throw new RuntimeException("Cannot merge with error schedule !");
	}

	Map<Instance, List<Job>> newJobsDistribution = new HashMap<Instance, List<Job>>();
	int onDemandInstanceCount = 0;
	int reservedInstanceCount = 0;

	List<Instance> reservedInstances = new ArrayList<Instance>();

	// Copy over the jobs and the instances from this
	for (Instance instance : this.jobsDistribution.keySet()) {
	    if (instance.isReserved()) {
		Instance r = new Instance(reservedInstanceCount, true);
		reservedInstanceCount++;
		newJobsDistribution.put(r, this.jobsDistribution.get(instance));
		//
		reservedInstances.add(r);
	    } else {
		Instance i = new Instance(onDemandInstanceCount, false);
		onDemandInstanceCount++;
		newJobsDistribution.put(i, this.jobsDistribution.get(instance));
	    }
	}
	// Add the OnDemand instance from next schedule and shift all the jobs
	// forward for about this.finalTime
	int reservedInstanceCountFromSchedule = 0;

	for (Instance instance : schedule.jobsDistribution.keySet()) {
	    if (instance.isReserved()) {
		Instance r = null;
		// we have a matching instance
		if (reservedInstanceCountFromSchedule < reservedInstanceCount) {
		    r = reservedInstances.get(reservedInstanceCountFromSchedule);
		} else {
		    // We create a new one and register
		    r = new Instance(reservedInstanceCount, true);
		    // Global counter
		    reservedInstanceCount++;
		    newJobsDistribution.put(r, new ArrayList<Job>());
		}
		// FIXME CLone or deep copy is better
		// Copy the original in the new one
		for (Job job : schedule.jobsDistribution.get(instance)) {
		    Job j = Job.createNewJob(job);
		    j.startTime = j.startTime + finalTime;
		    j.endTime = j.endTime + finalTime;
		    j.deployedOnInstance = r.getId();
		    newJobsDistribution.get(r).add(j);
		}
		// we add the shifthed jobs
		// local counter
		reservedInstanceCountFromSchedule++;
	    } else {
		Instance i = new Instance(onDemandInstanceCount, false);
		onDemandInstanceCount++;
		newJobsDistribution.put(i, new ArrayList<Job>());
		// FIXME CLone or deep copy is better
		// Copy the original in the new one
		for (Job job : schedule.jobsDistribution.get(instance)) {
		    Job j = Job.createNewJob(job);
		    j.startTime = j.startTime + finalTime;
		    j.endTime = j.endTime + finalTime;
		    j.deployedOnInstance = i.getId();
		    newJobsDistribution.get(i).add(j);
		}

	    }
	}
	// Update current Schedule
	this.jobsDistribution = newJobsDistribution;
	// Update all the other parameters (computation time does not really
	// make sense at this point unless we do not want to accumate that as
	// well
	// This represents the cumulative measure ! This would be differnt that
	// recomputing everything at the end
	finalTime = finalTime + schedule.getFinalTime();

	finalCost = finalCost + schedule.getFinalCost(); // This one is the one
							 // we care, since
							 // reserved instance
							 // costs is FIXED !!!
	objective = objective + schedule.objective;
    }

}
