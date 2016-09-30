package de.unisaarland.cs.st.schedulers;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.mappers.BasicMapper;
import de.unisaarland.cs.st.util.JobConverter;

public class MergeScheduleTest {

    Set<TestJob> testJobs;
    Set<Image> availableImages;
    CloudModel cloudModel;
    Goal goal;
    Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping;
    Set<Job> jobsToSchedule;
    //

    private Set<Job> convertJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> partialSolution,
	    CloudModel cloudModel) {
	return JobConverter.toJobs(partialSolution, cloudModel);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testMergeWithError() throws FileNotFoundException, YamlException {
	System.out.println("AbstractSchedulerTest.setup()");

	String testJobsFile = "src/test/resources/test-jobs-real.yml";
	YamlReader testJobsReader = new YamlReader(new FileReader(testJobsFile));
	this.testJobs = testJobsReader.read(Set.class, TestJob.class);

	String availableImageFile = "src/test/resources/available-images-real.yml";
	YamlReader availableImageReader = new YamlReader(new FileReader(availableImageFile));
	this.availableImages = availableImageReader.read(Set.class, Image.class);

	Image baseImage = Image.getEmptyImage();

	String cloudModelFile = "src/test/resources/cloud-model.yml";
	YamlReader cloudModelReader = new YamlReader(new FileReader(cloudModelFile));
	this.cloudModel = cloudModelReader.read(CloudModel.class);
	//
	String goalFile = "src/test/resources/goal.yml";
	YamlReader goalReader = new YamlReader(new FileReader(goalFile));
	this.goal = goalReader.read(Goal.class);

	// Compute the mapping using the Basic Mapper -> All using the
	// Base Image and no snapshot
	mapping = new BasicMapper().solve(testJobs, availableImages, baseImage, cloudModel, goal);
	//
	jobsToSchedule = convertJobs(mapping, cloudModel);
	//
	ITestJobScheduler scheduler1 = new RandomScheduler();
	//
	Schedule schedule1 = scheduler1.solve(jobsToSchedule, cloudModel, goal);
	Schedule schedule2 = Schedule.ERROR_SCHEDULE;

	System.out.println("MergeScheduleTest.testMerge() Schedule 1\n" + schedule1);
	System.out.println("MergeScheduleTest.testMerge() Schedule 2\n " + schedule2);
	schedule1.mergeWith(schedule2);
    }

    @Test
    public void testMergeWithEmpty() throws FileNotFoundException, YamlException {
	System.out.println("AbstractSchedulerTest.setup()");

	String testJobsFile = "src/test/resources/test-jobs-real.yml";
	YamlReader testJobsReader = new YamlReader(new FileReader(testJobsFile));
	this.testJobs = testJobsReader.read(Set.class, TestJob.class);

	String availableImageFile = "src/test/resources/available-images-real.yml";
	YamlReader availableImageReader = new YamlReader(new FileReader(availableImageFile));
	this.availableImages = availableImageReader.read(Set.class, Image.class);

	Image baseImage = Image.getEmptyImage();

	String cloudModelFile = "src/test/resources/cloud-model.yml";
	YamlReader cloudModelReader = new YamlReader(new FileReader(cloudModelFile));
	this.cloudModel = cloudModelReader.read(CloudModel.class);
	//
	String goalFile = "src/test/resources/goal.yml";
	YamlReader goalReader = new YamlReader(new FileReader(goalFile));
	this.goal = goalReader.read(Goal.class);

	// Compute the mapping using the Basic Mapper -> All using the
	// Base Image and no snapshot
	mapping = new BasicMapper().solve(testJobs, availableImages, baseImage, cloudModel, goal);
	//
	jobsToSchedule = convertJobs(mapping, cloudModel);
	//
	ITestJobScheduler scheduler1 = new RandomScheduler();
	//
	Schedule schedule1 = scheduler1.solve(jobsToSchedule, cloudModel, goal);
	Schedule schedule2 = Schedule.newEmptySchedule();

	System.out.println("MergeScheduleTest.testMerge() Schedule 1\n" + schedule1);
	System.out.println("MergeScheduleTest.testMerge() Schedule 2\n " + schedule2);
	schedule1.mergeWith(schedule2);
	System.out.println("MergeScheduleTest.testMerge() MERGED\n" + "" + schedule1);
    }

    @Test
    public void testMerge() throws FileNotFoundException, YamlException {
	System.out.println("AbstractSchedulerTest.setup()");

	String testJobsFile = "src/test/resources/test-jobs-real.yml";
	YamlReader testJobsReader = new YamlReader(new FileReader(testJobsFile));
	this.testJobs = testJobsReader.read(Set.class, TestJob.class);

	String availableImageFile = "src/test/resources/available-images-real.yml";
	YamlReader availableImageReader = new YamlReader(new FileReader(availableImageFile));
	this.availableImages = availableImageReader.read(Set.class, Image.class);

	Image baseImage = Image.getEmptyImage();

	String cloudModelFile = "src/test/resources/cloud-model.yml";
	YamlReader cloudModelReader = new YamlReader(new FileReader(cloudModelFile));
	this.cloudModel = cloudModelReader.read(CloudModel.class);
	//
	String goalFile = "src/test/resources/goal.yml";
	YamlReader goalReader = new YamlReader(new FileReader(goalFile));
	this.goal = goalReader.read(Goal.class);

	// Compute the mapping using the Basic Mapper -> All using the
	// Base Image and no snapshot
	mapping = new BasicMapper().solve(testJobs, availableImages, baseImage, cloudModel, goal);
	//
	jobsToSchedule = convertJobs(mapping, cloudModel);
	//
	ITestJobScheduler scheduler1 = new RandomScheduler();
	ITestJobScheduler scheduler2 = new RandomScheduler();
	//
	Schedule schedule1 = scheduler1.solve(jobsToSchedule, cloudModel, goal);
	Schedule schedule2 = scheduler2.solve(jobsToSchedule, cloudModel, goal);

	System.out.println("MergeScheduleTest.testMerge() Schedule 1\n" + schedule1);
	System.out.println("MergeScheduleTest.testMerge() Schedule 2\n " + schedule2);
	schedule1.mergeWith(schedule2);
	System.out.println("MergeScheduleTest.testMerge() MERGED\n" + "" + schedule1);
    }

}
