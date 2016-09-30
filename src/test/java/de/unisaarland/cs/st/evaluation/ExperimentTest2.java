package de.unisaarland.cs.st.evaluation;

import java.io.FileNotFoundException;

import org.apache.commons.cli.ParseException;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;

import de.unisaarland.cs.st.data.Image;

public class ExperimentTest2 {

    final static String OPT = "--stop-on-error --planners %s --test-jobs %s --available-images %s --base-image-id %s --cloud-model %s --goal %s";

    String planners = "" //
	    + "de.unisaarland.cs.st.planners.SequentialPlanner,"
	    + "de.unisaarland.cs.st.planners.SequentialPlannerWithOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.SequentialPlannerWithOpportunisticSnapshotOffLine,"
	    + "de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshotOffLine,"
	    + "de.unisaarland.cs.st.planners.MaxParallelismPlanner,"
	    + "de.unisaarland.cs.st.planners.MaxParallelismPlannerWithOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.MaxParallelismPlannerWithOpportunisticSnapshotOffLine,"
	    + "de.unisaarland.cs.st.planners.MaxParallelismPlannerWithFastOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.MaxParallelismPlannerWithFastOpportunisticSnapshotOffLine,"
	    + "de.unisaarland.cs.st.planners.MinLoadPlanner,"
	    + "de.unisaarland.cs.st.planners.MinLoadPlannerWithOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.MinLoadPlannerWithOpportunisticSnapshotOffLine,"
	    + "de.unisaarland.cs.st.planners.MinLoadPlannerWithFastOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.MinLoadPlannerWithFastOpportunisticSnapshotOffLine,"
	    + "de.unisaarland.cs.st.planners.RandomPlanner,"
	    + "de.unisaarland.cs.st.planners.RandomPlannerWithOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.RandomPlannerWithOpportunisticSnapshotOffLine,"
	    + "de.unisaarland.cs.st.planners.RandomPlannerWithFastOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.RandomPlannerWithFastOpportunisticSnapshotOffLine,"
	    + "de.unisaarland.cs.st.planners.RoundRobinPlanner,"
	    + "de.unisaarland.cs.st.planners.RoundRobinPlannerWithOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.RoundRobinPlannerWithOpportunisticSnapshotOffLine,"
	    + "de.unisaarland.cs.st.planners.RoundRobinPlannerWithFastOpportunisticSnapshot,"
	    + "de.unisaarland.cs.st.planners.RoundRobinPlannerWithFastOpportunisticSnapshotOffLine";
    //
    String testJobs = "src/test/resources/test-jobs-real.yml";
    String availableImages = "src/test/resources/available-images-real.yml";
    String baseImageId = "1443685462";
    String cloudModel = "src/test/resources/cloud-model.yml";
    String goal = "src/test/resources/goal.yml";

    @Test
    public void mainTestWithEmptyImage() throws FileNotFoundException, YamlException, ParseException {
	//
	baseImageId = Image.getEmptyImage().name;
	EvaluationDriver.main(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
    }

    @Test
    public void mainTestWithEmptyImageOffLine() throws FileNotFoundException, YamlException, ParseException {
	//
	String planners = "" //
		+ "de.unisaarland.cs.st.planners.MaxParallelismPlanner,"
		+ "de.unisaarland.cs.st.planners.MaxParallelismPlannerWithOpportunisticSnapshot,"
		+ "de.unisaarland.cs.st.planners.MaxParallelismPlannerWithOpportunisticSnapshotOffLine,"
		+ "de.unisaarland.cs.st.planners.MaxParallelismPlannerWithFastOpportunisticSnapshot,"
		+ "de.unisaarland.cs.st.planners.MaxParallelismPlannerWithFastOpportunisticSnapshotOffLine";
	baseImageId = Image.getEmptyImage().name;
	EvaluationDriver.main(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
    }

    @Test
    public void mainTestWithBaseImage() throws FileNotFoundException, YamlException, ParseException {
	//
	EvaluationDriver.main(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
    }
}
