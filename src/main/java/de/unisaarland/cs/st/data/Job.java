package de.unisaarland.cs.st.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Job implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7575626245214411073L;
    private static AtomicInteger idGenerator = new AtomicInteger(0);

    public static Job createNewJob(long processingTime) {
	return new Job(idGenerator.incrementAndGet(), Long.MIN_VALUE, Long.MIN_VALUE, processingTime,
		new HashSet<Job>());

    }

    public int id;
    public long startTime;
    public long endTime;
    //
    public long processingTime;
    //
    //
    public int deployedOnInstance = -1;
    public boolean deployedOnReservedInstance = false;

    public Set<Job> dependsOn;
    public boolean snapshot;
    public Image parentImage;
    //
    public Image image;
    public TestJob testJob;
    public long delta;

    // This is needed for YAML writer/reader
    public Job() {
    }

    private Job(int id, long startTime, long endTime, long processingTime, Set<Job> dependsOn) {
	super();
	this.id = id;
	this.startTime = startTime;
	this.endTime = endTime;
	this.processingTime = processingTime;
	this.dependsOn = dependsOn;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((dependsOn == null) ? 0 : dependsOn.hashCode());
	result = prime * result + (int) (endTime ^ (endTime >>> 32));
	result = prime * result + id;
	result = prime * result + (int) (processingTime ^ (processingTime >>> 32));
	result = prime * result + (int) (startTime ^ (startTime >>> 32));
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Job other = (Job) obj;
	if (dependsOn == null) {
	    if (other.dependsOn != null)
		return false;
	} else if (!dependsOn.equals(other.dependsOn))
	    return false;
	if (endTime != other.endTime)
	    return false;
	if (id != other.id)
	    return false;
	if (processingTime != other.processingTime)
	    return false;
	if (startTime != other.startTime)
	    return false;
	return true;
    }

    public static Job createNewJob(Job job) {
	// This is the tricky part !
	Job j = Job.createNewJob(job.processingTime);
	j.snapshot = job.snapshot;
	j.delta = job.delta;
	j.dependsOn = job.dependsOn;
	j.deployedOnInstance = job.deployedOnInstance;
	j.deployedOnReservedInstance = job.deployedOnReservedInstance;
	j.image = job.image;
	j.parentImage = job.parentImage;
	j.snapshot = job.snapshot;
	j.testJob = job.testJob;
	// Shift the job
	j.startTime = job.startTime;
	j.endTime = job.endTime;
	//
	return j;
    }

}