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
import de.unisaarland.cs.st.schedulers.RandomScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;
import de.unisaarland.cs.st.util.JobConverter;

public class RandomPlannerWithOpportunisticSnapshotOffLine extends TestExecutionPlanner {

    @Override
    public Set<Job> convertTestJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping,
	    CloudModel cloudModel) {
	return JobConverter.toJobs(mapping, cloudModel, false);
    }

    public RandomPlannerWithOpportunisticSnapshotOffLine() {
	super("RandomPlannerWithOpportunisticSnapshotOffLine", new CachedOpportunisticSnapshot(), new RandomScheduler(),
		new BasicInputDataValidator());
    }

}
