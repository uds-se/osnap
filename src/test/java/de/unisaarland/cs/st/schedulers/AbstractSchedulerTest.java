package de.unisaarland.cs.st.schedulers;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
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

public abstract class AbstractSchedulerTest {

    Set<TestJob> testJobs;
    Set<Image> availableImages;
    CloudModel cloudModel;
    Goal goal;
    Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping;
    Set<Job> jobsToSchedule;
    //
    ITestJobScheduler scheduler;
    // schedule

    @BeforeMethod
    public void setup() throws YamlException, FileNotFoundException {
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

    }

    @Test
    public void testTheScheduler() {
	Schedule schedule = scheduler.solve(jobsToSchedule, cloudModel, goal);
	makeAssertionOnSchedule(schedule);
    }

    // TODO Is this really needed ? Sometimes Schedulers have constraints and must invoker
    public Set<Job> convertJobs(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> partialSolution,
	    CloudModel cloudModel) {
	return JobConverter.toJobs(partialSolution, cloudModel);
    }

    public abstract void setSchedulerAsBeforeMethod();

    abstract void makeAssertionOnSchedule(Schedule schedule);
}
