package de.unisaarland.cs.st.planners;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.mappers.CachedOpportunisticSnapshot;
import de.unisaarland.cs.st.schedulers.MaxParallelismScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;
import de.unisaarland.cs.st.util.JobConverter;

/**
 * 
 * This Test Execution Planner implements the maximum parallelism planner that
 * is 1 test job for 1 instance (regarless instances are reseved or not!)
 * 
 * @author gambi
 *
 */
public class MaxParallelismPlannerWithOpportunisticSnapshot extends TestExecutionPlanner {

    public static final String NAME = "MaxParallelismPlannerWithOpportunisticSnapshot";

    // TODO Probably not necessary since MaxParallale will implement the
    // phase1+phase2 heuristic
    @Override
    public Set<Job> convertTestJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping,
	    CloudModel cloudModel) {
	return JobConverter.toOderedJobs(mapping, cloudModel);
    }

    public MaxParallelismPlannerWithOpportunisticSnapshot() {
	super(NAME, new CachedOpportunisticSnapshot(), new MaxParallelismScheduler(), new BasicInputDataValidator());

    }

}
