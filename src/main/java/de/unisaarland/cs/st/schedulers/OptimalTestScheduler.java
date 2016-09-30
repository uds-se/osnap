package de.unisaarland.cs.st.schedulers;

import java.util.Set;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;

public class OptimalTestScheduler extends ILPScheduler {

    int maxParallelism;

    @Override
    public Schedule solve(Set<Job> jobs, CloudModel cloudModel, Goal goal) {
	this.maxParallelism = inputJobs.size();
	return super.solve(jobs, cloudModel, goal);
    }

    //
    @Override
    protected void setMaxOnDemandInstances() {
	logger.warn("Setting maxOnDemandInstances to " + maxParallelism);
	this.maxOnDemandInstances = maxParallelism;
    }

}
