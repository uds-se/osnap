package de.unisaarland.cs.st.data;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

public class TestJobTest {

    String availableImageFile = "src/test/resources/available-images.yml";

    @Test
    public void equalsAndHashCodeTestForAvailableImages() throws YamlException, FileNotFoundException {
	String testJobsFile = "src/test/resources/test-jobs.yml";
	YamlReader testJobsReader = new YamlReader(new FileReader(testJobsFile));
	Set<TestJob> testJobs1 = testJobsReader.read(Set.class, TestJob.class);
	//
	testJobsReader = new YamlReader(new FileReader(testJobsFile));
	Set<TestJob> testJobs2 = testJobsReader.read(Set.class, TestJob.class);

	// Image baseImage = Image.getEmptyImage();
	Assert.assertEquals(testJobs1, testJobs1);
	//
	Assert.assertEquals(testJobs1, testJobs2);
	//
	Assert.assertEquals(testJobs1.hashCode(), testJobs2.hashCode());
    }

    @Test
    public void equalsAndHashCodeTest() throws YamlException, FileNotFoundException {
	String testJobsFile = "src/test/resources/test-jobs.yml";
	YamlReader testJobsReader = new YamlReader(new FileReader(testJobsFile));
	Set<TestJob> testJobs1 = testJobsReader.read(Set.class, TestJob.class);
	TestJob testJob1 = testJobs1.iterator().next();
	//
	testJobsReader = new YamlReader(new FileReader(testJobsFile));
	Set<TestJob> testJobs2 = testJobsReader.read(Set.class, TestJob.class);
	TestJob testJob2 = testJobs2.iterator().next();

	// This might not work since set ordering cannot be enforced

	Assert.assertEquals(testJob1, testJob1);
	//
	Assert.assertTrue(testJobs1.contains(testJob2));
	Assert.assertTrue(testJobs2.contains(testJob1));
    }

}
