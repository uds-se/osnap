package de.unisaarland.cs.st.planners;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.mappers.CachedOpportunisticSnapshot;
import de.unisaarland.cs.st.schedulers.MinLoadScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;
import de.unisaarland.cs.st.util.JobConverter;

public class MinLoadPlannerWithOpportunisticSnapshotOffLine extends TestExecutionPlanner {

    public static final String NAME = "MinLoadPlannerWithOpportunisticSnapshotOffLine";

    @Override
    public Set<Job> convertTestJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping,
	    CloudModel cloudModel) {
	// Force Off Line Model
	return JobConverter.toJobs(mapping, cloudModel, false);
    }

    public MinLoadPlannerWithOpportunisticSnapshotOffLine() {
	super(NAME, new CachedOpportunisticSnapshot(), new MinLoadScheduler(), new BasicInputDataValidator());

    }

}
