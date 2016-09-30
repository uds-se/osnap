package de.unisaarland.cs.st.planners;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.mappers.BasicMapper;
import de.unisaarland.cs.st.schedulers.SequentialScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;
import de.unisaarland.cs.st.util.JobConverter;

/**
 * 
 * @author gambi
 *
 */
public class SequentialPlanner extends TestExecutionPlanner {

    public final static String NAME = "SequentialPlanner";

    public SequentialPlanner() {
	super(NAME, new BasicMapper(), new SequentialScheduler(), new BasicInputDataValidator());
    }

    // Sequential requires jobs already lined up
    public Set<Job> convertTestJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping,
	    CloudModel cloudModel) {
	return JobConverter.toOderedJobs(mapping, cloudModel);
    }

}
