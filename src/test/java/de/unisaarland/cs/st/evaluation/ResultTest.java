package de.unisaarland.cs.st.evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.cli.ParseException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;

import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.evaluation.resultprocessors.PackageTiming;
import de.unisaarland.cs.st.evaluation.resultprocessors.StdOutPackageTiming;

public class ResultTest {

    private Result result;

    @BeforeMethod
    public void computeResult() throws YamlException, FileNotFoundException, ParseException {
	String planners = "de.unisaarland.cs.st.planners.SequentialPlanner,de.unisaarland.cs.st.planners.SequentialPlannerWithOpportunisticSnapshot,de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshot";
	String testJobs = "src/test/resources/test-jobs-real.yml";
	String availableImages = "src/test/resources/available-images-real.yml";
	String baseImageId = Image.getEmptyImage().name;
	String cloudModel = "src/test/resources/cloud-model.yml";
	String goal = "src/test/resources/goal.yml";

	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(ExperimentTest1.OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal)
			.split(" "));
	//
	result = driver.execute();

    }

    @Test
    public void mergeResult() throws YamlException, FileNotFoundException, ParseException {
	String planners = "de.unisaarland.cs.st.planners.SequentialPlanner";
	String testJobs = "src/test/resources/test-jobs-real.yml";
	String availableImages = "src/test/resources/available-images-real.yml";
	String baseImageId = Image.getEmptyImage().name;
	String cloudModel = "src/test/resources/cloud-model.yml";
	String goal = "src/test/resources/goal.yml";

	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(ExperimentTest1.OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal)
			.split(" "));
	//
	long finalTime = result.getSchedules().get("SequentialPlanner").getFinalTime();

	Result result1 = driver.execute();
	result1.mergeWith(result);
	// Assert ?
	System.out.println("ResultTest.mergeResult() " + result.getPlannerNames().size());
	System.out.println("ResultTest.mergeResult() " + result1.getPlannerNames().size());
	//
	long mergedFinalTime = result1.getSchedules().get("SequentialPlanner").getFinalTime();
	System.out.println("ResultTest.mergeResult() " + finalTime + "  " + mergedFinalTime);
    }

    @Test
    public void resultReaderTestNoResultProcessor() throws IOException, ParseException, ClassNotFoundException {

	// Store to File
	File tempFile = File.createTempFile("test", "osnap");
	tempFile.deleteOnExit();
	// File tempFile = new File(
	// "/Users/gambi/ICST2017/icst2017-osnap/evaluation/experiment_1_A/20151001T094422Z/010/001/results");
	//
	System.out.println("ResultTest.serializeTest() Serialize to " + tempFile);
	FileOutputStream fout = new FileOutputStream(tempFile);
	ObjectOutputStream oos = new ObjectOutputStream(fout);
	oos.writeObject(result);
	//
	Assert.assertTrue(tempFile.length() > 0);
	//
	ResultReader reader = new ResultReader();
	reader.parseArgs(new String[] { "--input-file", tempFile.getAbsolutePath() });
	reader.execute();
    }

    @Test
    public void resultReaderTestWithResultProcessor() throws IOException, ClassNotFoundException, ParseException {

	// Store to File
	File tempFile = File.createTempFile("test", "osnap");
	tempFile.deleteOnExit();
	//
	System.out.println("ResultTest.serializeTest() Serialize to " + tempFile);
	FileOutputStream fout = new FileOutputStream(tempFile);
	ObjectOutputStream oos = new ObjectOutputStream(fout);
	oos.writeObject(result);
	//
	Assert.assertTrue(tempFile.length() > 0);
	//
	ResultReader reader = new ResultReader();
	reader.parseArgs(new String[] { "--input-file", tempFile.getAbsolutePath(), "--result-processor",
		StdOutPackageTiming.class.getName() });
	reader.execute();
    }

    @Test
    public void resultReaderTestWithResultProcessor2() throws IOException, ClassNotFoundException, ParseException {

	// Store to File
	File tempFile = File.createTempFile("test", "osnap");
	tempFile.deleteOnExit();
	//
	System.out.println("ResultTest.serializeTest() Serialize to " + tempFile);
	FileOutputStream fout = new FileOutputStream(tempFile);
	ObjectOutputStream oos = new ObjectOutputStream(fout);
	oos.writeObject(result);
	//
	Assert.assertTrue(tempFile.length() > 0);
	//
	ResultReader reader = new ResultReader();
	reader.parseArgs(new String[] { "--input-file", tempFile.getAbsolutePath(), "--result-processor",
		PackageTiming.class.getName() });
	reader.execute();
    }

    @Test
    public void serializeTest() throws IOException {

	// Store to File
	File tempFile = File.createTempFile("test", "osnap");
	tempFile.deleteOnExit();
	//
	System.out.println("ResultTest.serializeTest() Serialize to " + tempFile);
	FileOutputStream fout = new FileOutputStream(tempFile);
	ObjectOutputStream oos = new ObjectOutputStream(fout);
	oos.writeObject(result);
	//
	Assert.assertTrue(tempFile.length() > 0);
    }

    @Test
    public void deserializeTest() throws IOException, ClassNotFoundException {
	// Store to File
	File tempFile = File.createTempFile("test", "osnap");
	tempFile.deleteOnExit();
	// Serialize
	FileOutputStream fout = new FileOutputStream(tempFile);
	ObjectOutputStream oos = new ObjectOutputStream(fout);
	oos.writeObject(result);
	//
	System.out.println("ResultTest.serializeTest() Serialize to " + tempFile);
	//
	FileInputStream fin = new FileInputStream(tempFile);
	ObjectInputStream ois = new ObjectInputStream(fin);
	Result result2 = (Result) ois.readObject();
	// Equals - A bit weak
	Assert.assertEquals(result2.planners, result.planners);
	Assert.assertEquals(result.schedules.size(), result2.schedules.size());
	Assert.assertEquals(result.availableImages, result2.availableImages);
    }
}
