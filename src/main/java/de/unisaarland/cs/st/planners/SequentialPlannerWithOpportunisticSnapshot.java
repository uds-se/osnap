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
import de.unisaarland.cs.st.schedulers.SequentialScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;
import de.unisaarland.cs.st.util.JobConverter;

/**
 * This Test Execution Planner implements the full approach: OSnap +
 * SequentialScheduling
 * 
 * @author gambi
 *
 */
public class SequentialPlannerWithOpportunisticSnapshot extends TestExecutionPlanner {

    public static final String NAME = "SequentialPlannerWithOpportunisticSnapshot";

    public SequentialPlannerWithOpportunisticSnapshot() {
	super(NAME, new CachedOpportunisticSnapshot(), new SequentialScheduler(), new BasicInputDataValidator());
    }

    public Set<Job> convertTestJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping,
	    CloudModel cloudModel) {
	return JobConverter.toOderedJobs(mapping, cloudModel);
    }
}
