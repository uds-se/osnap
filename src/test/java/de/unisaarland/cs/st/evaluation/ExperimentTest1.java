package de.unisaarland.cs.st.evaluation;

import java.io.FileNotFoundException;

import org.apache.commons.cli.ParseException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;

import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.evaluation.resultprocessors.ChainedResultProcessor;
import de.unisaarland.cs.st.evaluation.resultprocessors.StdOutPackageTiming;
import de.unisaarland.cs.st.evaluation.resultprocessors.StdOutResultProcessor;
import de.unisaarland.cs.st.evaluation.resultprocessors.StdOutScheduleProcessor;

public class ExperimentTest1 {

    final static String OPT = "--stop-on-error --planners %s --test-jobs %s --available-images %s --base-image-id %s --cloud-model %s --goal %s";
    // final static String OPT_WITH_JAVA_OPTION = "-D log4j.debug=true
    // --planners %s --test-jobs %s --available-images %s --base-image-id %s
    // --cloud-model %s --goal %s";
    // final static String OPT_WITH_JAVA_OPTIONS = "-D log4j.debug=true -D
    // anotherOption=anotherValue --planners %s --test-jobs %s
    // --available-images %s --base-image-id %s --cloud-model %s --goal %s";
    final static String OPT_WITH_RESULT_PROCESSOR = "--stop-on-error  --planners %s --test-jobs %s --available-images %s --base-image-id %s --cloud-model %s --goal %s --result-processor %s";

    String planners = "de.unisaarland.cs.st.planners.SequentialPlanner,de.unisaarland.cs.st.planners.SequentialPlannerWithOpportunisticSnapshot,de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.ILPPlanner,de.unisaarland.cs.st.planners.ILPPlannerWithOpportunisticSnapshot,de.unisaarland.cs.st.planners.ILPPlannerWithFastOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.MinLoadPlanner,de.unisaarland.cs.st.planners.MinLoadPlannerWithOpportunisticSnapshot,de.unisaarland.cs.st.planners.MinLoadPlannerWithFastOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.RoundRobinPlanner,de.unisaarland.cs.st.planners.RoundRobinPlannerWithOpportunisticSnapshot,de.unisaarland.cs.st.planners.RoundRobinPlannerWithFastOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.RandomPlanner,de.unisaarland.cs.st.planners.RandomPlannerWithOpportunisticSnapshot,de.unisaarland.cs.st.planners.RandomPlannerWithFastOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.MaxParallelismPlanner,de.unisaarland.cs.st.planners.MaxParallelismPlannerWithOpportunisticSnapshot,de.unisaarland.cs.st.planners.MaxParallelismPlannerWithFastOpportunisticSnapshot,"
	    + "" + "" + "" + "";

    String testJobs = "src/test/resources/test-jobs-real.yml";
    String availableImages = "src/test/resources/available-images-real.yml";
    String baseImageId = Image.getEmptyImage().name;
    String cloudModel = "src/test/resources/cloud-model.yml";
    String goal = "src/test/resources/goal.yml";

    @Test
    public void mainRealDataAndNegativeWeightImage() throws FileNotFoundException, YamlException, ParseException {
	//
	String planners = "de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshot";
	String testJobs = "src/test/resources/test-jobs-20151001T094422Z.yml";
	String availableImages = "src/test/resources/available-images-20151001T094422Z.yml";
	String baseImageId = "1443685462";
	String cloudModel = "src/test/resources/cloud-model-20151001T094422Z.yml";
	String goal = "src/test/resources/goal-20151001T094422Z.yml";
	//
	EvaluationDriver.main(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
    }

    @Test
    public void mainRealDataAndMissingSnapshotLink() throws FileNotFoundException, YamlException, ParseException {
	//
	String planners = "de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshot";
	String testJobs = "src/test/resources/test-jobs-error.yml";
	String availableImages = "src/test/resources/available-images-error.yml";
	String baseImageId = "1449414703";
	String cloudModel = "src/test/resources/cloud-model.yml";
	String goal = "src/test/resources/goal.yml";
	//
	EvaluationDriver.main(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
    }

    @Test
    public void mainRealDataAndMissingTestNodeLink() throws FileNotFoundException, YamlException, ParseException {
	//
	String planners = "de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshot";
	String testJobs = "src/test/resources/test-jobs-error2.yml";
	String availableImages = "src/test/resources/available-images-error.yml";
	String baseImageId = "1449414703";
	String cloudModel = "src/test/resources/cloud-model.yml";
	String goal = "src/test/resources/goal.yml";
	//
	EvaluationDriver.main(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
    }

    @Test
    public void mainRealDataAndNegativeObjective() throws FileNotFoundException, YamlException, ParseException {
	//
	String planners = "de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshot";
	String testJobs = "src/test/resources/test-jobs-20151001T094422Z-007.yml";
	String availableImages = "src/test/resources/available-images-20151001T094422Z.yml";
	String baseImageId = "1443685462";
	String cloudModel = "src/test/resources/cloud-model-20151001T094422Z.yml";
	String goal = "src/test/resources/goal-20151001T094422Z.yml";
	//
	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
	Result result = driver.execute();
	for (Schedule schedule : result.schedules.values()) {
	    Assert.assertTrue(schedule.objective >= 0, "Schedule object is Negative !!");
	    Assert.assertTrue(schedule.getFinalTime() >= 0, "Schedule time is Negative !!");
	    Assert.assertTrue(schedule.getFinalCost() >= 0, "Schedule cost is Negative !!");
	}
    }

    @Test
    public void mainRealDataAndMissingTestNode() throws FileNotFoundException, YamlException, ParseException {
	//
	String planners = "de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshot";
	String testJobs = "src/test/resources/test-jobs-20151001T094422Z-003.yml";
	String availableImages = "src/test/resources/available-images-20151001T094422Z.yml";
	String baseImageId = "1443685462";
	String cloudModel = "src/test/resources/cloud-model-20151001T094422Z.yml";
	String goal = "src/test/resources/goal-20151001T094422Z.yml";
	//
	EvaluationDriver.main(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
    }

    //
    @Test
    public void mainRealDataAndBaseImageCannotBeAdded() throws FileNotFoundException, YamlException, ParseException {
	//
	String planners = "de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshot";
	String testJobs = "src/test/resources/test-jobs-20151001T094422Z-006.yml";
	String availableImages = "src/test/resources/available-images-20151001T094422Z.yml";
	String baseImageId = "1443685462";
	String cloudModel = "src/test/resources/cloud-model-20151001T094422Z.yml";
	String goal = "src/test/resources/goal-20151001T094422Z.yml";
	//
	EvaluationDriver.main(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
    }

    @Test
    public void mainTestWithEmptyImage() throws FileNotFoundException, YamlException, ParseException {
	//
	EvaluationDriver.main(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
    }

    @Test
    public void mainTestWithEmptyImageILPPlanner() throws FileNotFoundException, YamlException, ParseException {
	//
	String planners = "de.unisaarland.cs.st.planners.ILPPlanner,de.unisaarland.cs.st.planners.ILPPlannerWithOpportunisticSnapshot,de.unisaarland.cs.st.planners.ILPPlannerWithFastOpportunisticSnapshot";
	// de.unisaarland.cs.st.planners.ILPPlannerWithOpportunisticSnapshotOffLine

	// de.unisaarland.cs.st.planners.ILPPlannerWithFastOpportunisticSnapshotOffLine
	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
	Result result = driver.execute();
	StdOutScheduleProcessor p = new StdOutScheduleProcessor();
	p.process(result);
    }

    @Test
    public void mainTestWithEmptyImageMaxParallelismPlanner()
	    throws FileNotFoundException, YamlException, ParseException {
	//
	String planners = "de.unisaarland.cs.st.planners.MaxParallelismPlanner,de.unisaarland.cs.st.planners.MaxParallelismPlannerWithOpportunisticSnapshot,de.unisaarland.cs.st.planners.MaxParallelismPlannerWithFastOpportunisticSnapshot";
	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
	Result result = driver.execute();
	StdOutScheduleProcessor p = new StdOutScheduleProcessor();
	p.process(result);
    }

    @Test
    public void mainTestWithEmptyImageOnlySequential() throws FileNotFoundException, YamlException, ParseException {
	EvaluationDriver.main(String.format(OPT, "de.unisaarland.cs.st.planners.SequentialPlanner", testJobs,
		availableImages, baseImageId, cloudModel, goal).split(" "));
    }

    @Test
    public void mainTestWithEmptyImageOnlyFastOSnap() throws FileNotFoundException, YamlException, ParseException {
	EvaluationDriver
		.main(String.format(OPT, "de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshot",
			testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
    }

    @Test
    public void mainTestWithEmptyImageWithPackageTimingResultProcessor()
	    throws FileNotFoundException, YamlException, ParseException {
	//
	String resultProcessorClassName = StdOutPackageTiming.class.getName();
	EvaluationDriver.main(String.format(OPT_WITH_RESULT_PROCESSOR, planners, testJobs, availableImages, baseImageId,
		cloudModel, goal, resultProcessorClassName).split(" "));
    }

    @Test
    public void mainTestWithBaseImageWithPackageTimingResultProcessor()
	    throws FileNotFoundException, YamlException, ParseException {
	//
	String resultProcessorClassName = StdOutPackageTiming.class.getName();
	String baseImageId = "1443685462";
	EvaluationDriver.main(String.format(OPT_WITH_RESULT_PROCESSOR, planners, testJobs, availableImages, baseImageId,
		cloudModel, goal, resultProcessorClassName).split(" "));
    }

    @Test
    public void testWithBaseImageWithChainedResultProcessor()
	    throws FileNotFoundException, YamlException, ParseException {
	//
	String baseImageId = "1443685462";
	//
	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
	ChainedResultProcessor chainedResultProcessor = new ChainedResultProcessor();
	chainedResultProcessor.addResultProcessor(new StdOutResultProcessor());
	chainedResultProcessor.addResultProcessor(new StdOutScheduleProcessor());
	chainedResultProcessor.addResultProcessor(new StdOutPackageTiming());
	//
	Result result = driver.execute();
	//
	chainedResultProcessor.process(result);

	// EvaluationDriver.main();
    }

    @Test
    public void testWithBaseImagePlus() throws FileNotFoundException, YamlException, ParseException {
	//
	String baseImageId = "1443685462";
	// This one contains all the packages for abi-compliance-checker few
	// more packages that are included in the test job, this shall make the
	// computation of
	//
	// getSetup(Base) and getSetup(Empty) slightly different

	String availableImages2 = "src/test/resources/available-images-real2.yml";
	//
	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(OPT, planners, testJobs, availableImages2, baseImageId, cloudModel, goal).split(" "));
	ChainedResultProcessor chainedResultProcessor = new ChainedResultProcessor();
	chainedResultProcessor.addResultProcessor(new StdOutScheduleProcessor());
	chainedResultProcessor.addResultProcessor(new StdOutPackageTiming());
	//
	Result result = driver.execute();
	//
	chainedResultProcessor.process(result);

	// EvaluationDriver.main();
    }

}
