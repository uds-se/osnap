package de.unisaarland.cs.st.planners;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import java.util.Set;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.mappers.CachedMapper;
import de.unisaarland.cs.st.mappers.ITestJobToVMMapper;
import de.unisaarland.cs.st.schedulers.ITestJobScheduler;
import de.unisaarland.cs.st.util.IInputDataValidator;
import de.unisaarland.cs.st.util.JobConverter;

public abstract class TestExecutionPlanner {

    private final Logger logger = Logger.getLogger(TestExecutionPlanner.class);

    private final ITestJobToVMMapper mapper;
    private final ITestJobScheduler scheduler;
    private final IInputDataValidator validator;
    private final String name;

    public TestExecutionPlanner(String name, ITestJobToVMMapper mapper, ITestJobScheduler scheduler,
	    IInputDataValidator validator) {
	this.name = name;
	this.mapper = mapper;
	this.scheduler = scheduler;
	this.validator = validator;
    }

    public IInputDataValidator getValidator() {
	return validator;
    }

    public ITestJobToVMMapper getMapper() {
	return mapper;
    }

    public ITestJobScheduler getScheduler() {
	return scheduler;
    }

    @Override
    public String toString() {
	return this.name;
    }

    public String getName() {
	return name;
    }

    //
    public Set<Job> convertTestJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping,
	    CloudModel cloudModel) {
	return JobConverter.toJobs(mapping, cloudModel);
    }

    public Schedule computeSchedule(Set<TestJob> testJobs, Set<Image> availableImages, Image baseImage,
	    CloudModel cloudModel, Goal goal) {
	// Validate the input data
	validator.validateInputData(testJobs, availableImages, baseImage, cloudModel, goal);

	// Here we start to measure the computation time
	long startComputationTime = System.currentTimeMillis();

	// Create a Deep Copy of the TestJobs ?
	Set<TestJob> _testJobs = new HashSet<TestJob>(testJobs.size());
	for (TestJob testJob : testJobs) {
	    // NOTE: Package will still be by-ref !
	    _testJobs.add(TestJob.deepCopy(testJob));
	}

	// Compute the mapping - if cached this will be fast therefore
	// computation time will be misleading
	Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping = mapper.solve(_testJobs, availableImages,
		baseImage, cloudModel, goal);

	// Adjust starting time
	if (mapper instanceof CachedMapper) {
	    logger.trace("Adjusting Timing for Cached Mapper");
	    startComputationTime = System.currentTimeMillis() - ((CachedMapper) mapper).getComputationTime(_testJobs,
		    availableImages, baseImage, cloudModel, goal);
	} else {
	    logger.trace("Mapper not Cached" + mapper.getClass());
	}

	// Enable some intermediate stuff ?
	long mappingComputationTime = System.currentTimeMillis() - startComputationTime;
	logger.debug("Mapping computation time " + mappingComputationTime);

	// TODO Somehow this should be inside scheduler ?
	// For some planner this must be implemented as toOrderedJob
	// Derive CloudJobs from Mapping vs toOrderedJobs ?
	Set<Job> jobs = convertTestJobs(mapping, cloudModel);

	// Compute the Final Schedule - and account for the mapping time as well
	// !
	Schedule finalSchedule = scheduler.solve(startComputationTime, jobs, cloudModel, goal);
	// Enable some intermediate stuff
	// long endTime = System.currentTimeMillis(); -- This should be alredy
	// included in solve !

	finalSchedule.baseImage = baseImage;
	return finalSchedule;

    }

}
