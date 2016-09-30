package de.unisaarland.cs.st.mappers;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.schedulers.ITestJobScheduler;

public class CachedOpportunisticSnapshotTest {

    private CachedOpportunisticSnapshot mapper;

    Set<TestJob> testJobs;
    Set<Image> availableImages;
    CloudModel cloudModel;
    Goal goal;
    Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping;
    Set<Job> jobsToSchedule;
    //
    ITestJobScheduler scheduler;

    @Test
    public void loadFromCache() throws FileNotFoundException, YamlException {
	mapper = new CachedOpportunisticSnapshot();
	mapper.clearCache();
	//
	String testJobsFile = "src/test/resources/test-jobs.yml";
	YamlReader testJobsReader = new YamlReader(new FileReader(testJobsFile));
	this.testJobs = testJobsReader.read(Set.class, TestJob.class);

	String availableImageFile = "src/test/resources/available-images.yml";
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
	//
	//
	Assert.assertFalse(mapper.cacheContains(testJobs, availableImages, baseImage, cloudModel, goal));

	Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping1 = mapper.compute(testJobs, availableImages,
		baseImage, cloudModel, goal);
	Assert.assertNotNull(mapping1);

	Assert.assertTrue(mapper.cacheContains(testJobs, availableImages, baseImage, cloudModel, goal));

	Assert.assertEquals(mapping1, mapper.getFromCache(testJobs, availableImages, baseImage, cloudModel, goal));
    }

    @Test
    public void loadFromAnotherCache() throws FileNotFoundException, YamlException {
	mapper = new CachedOpportunisticSnapshot();
	mapper.clearCache();
	//
	String testJobsFile = "src/test/resources/test-jobs.yml";
	YamlReader testJobsReader = new YamlReader(new FileReader(testJobsFile));
	this.testJobs = testJobsReader.read(Set.class, TestJob.class);

	String availableImageFile = "src/test/resources/available-images.yml";
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
	//
	// Store to cache
	Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping1 = mapper.compute(testJobs, availableImages,
		baseImage, cloudModel, goal);

	Assert.assertNotNull(mapping1);

	// Load and check from another instance of Cached Mapper
	CachedOpportunisticSnapshot mapper2 = new CachedOpportunisticSnapshot();
	Assert.assertTrue(mapper2.cacheContains(testJobs, availableImages, baseImage, cloudModel, goal));
    }

    @Test
    public void loadFromAnotherCache2() throws FileNotFoundException, YamlException {
	mapper = new CachedOpportunisticSnapshot();
	mapper.clearCache();
	//
	String testJobsFile = "src/test/resources/test-jobs.yml";
	YamlReader testJobsReader = new YamlReader(new FileReader(testJobsFile));
	this.testJobs = testJobsReader.read(Set.class, TestJob.class);

	String availableImageFile = "src/test/resources/available-images.yml";
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
	//
	// Store to cache
	Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping1 = mapper.compute(testJobs, availableImages,
		baseImage, cloudModel, goal);
	Assert.assertNotNull(mapping1);

	// Relaod everythign and check
	testJobsReader = new YamlReader(new FileReader(testJobsFile));
	Set<TestJob> testJobs2 = testJobsReader.read(Set.class, TestJob.class);

	availableImageReader = new YamlReader(new FileReader(availableImageFile));
	Set<Image> availableImages2 = availableImageReader.read(Set.class, Image.class);

	Image baseImage2 = Image.getEmptyImage();

	cloudModelReader = new YamlReader(new FileReader(cloudModelFile));
	CloudModel cloudModel2 = cloudModelReader.read(CloudModel.class);
	//

	goalReader = new YamlReader(new FileReader(goalFile));
	Goal goal2 = goalReader.read(Goal.class);
	//
	// Load and check from another instance of Cached Mapper
	CachedOpportunisticSnapshot mapper2 = new CachedOpportunisticSnapshot();
	Assert.assertTrue(mapper2.cacheContains(testJobs2, availableImages2, baseImage, cloudModel2, goal2));
    }
}
