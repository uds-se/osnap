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
import de.unisaarland.cs.st.schedulers.ILPScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;
import de.unisaarland.cs.st.util.JobConverter;

public class ILPPlannerWithOpportunisticSnapshotOffLine extends TestExecutionPlanner {

    @Override
    public Set<Job> convertTestJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping,
	    CloudModel cloudModel) {
	// Force OFF LINE
	return JobConverter.toOderedJobs(mapping, cloudModel, false);
    }

    public ILPPlannerWithOpportunisticSnapshotOffLine() {
	super("ILPPlannerWithOpportunisticSnapshotOffLine", new CachedOpportunisticSnapshot(), new ILPScheduler(),
		new BasicInputDataValidator());
    }
}
