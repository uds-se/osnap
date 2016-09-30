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
public class MaxParallelismPlannerWithOpportunisticSnapshotOffLine extends TestExecutionPlanner {

    public static final String NAME = "MaxParallelismPlannerWithOpportunisticSnapshotOffLine";

    @Override
    public Set<Job> convertTestJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping,
	    CloudModel cloudModel) {
	// Force Off line mode
	return JobConverter.toOderedJobs(mapping, cloudModel, false);
    }

    public MaxParallelismPlannerWithOpportunisticSnapshotOffLine() {
	super(NAME, new CachedOpportunisticSnapshot(), new MaxParallelismScheduler(), new BasicInputDataValidator());

    }

}
