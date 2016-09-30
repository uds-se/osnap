package de.unisaarland.cs.st.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.unisaarland.cs.st.planners.MaxParallelismPlanner;
import de.unisaarland.cs.st.planners.SequentialPlanner;
import de.unisaarland.cs.st.planners.SequentialPlannerWithOpportunisticSnapshot;
import de.unisaarland.cs.st.planners.TestExecutionPlanner;

public class EvaluationDriverTest {

    String baseFolder = "src/test/resources/";
    String realDataBaseFolder = "src/main/resources/";

    String availableImages = "available-images.yml";
    String baseImage = "Empty";

    String testJobs = "test-jobs.yml";
    // String realDataTestJobs = "test-jobs/per-day/20160412_000000.yml";

    String cloudModel = "cloud-model.yml";
    // String realDataCloudModel = "cloud-models/amazon-ec2-eu.yml";

    String goal = "goal.yml";
    String readDataGoal = "goals/goal_80_20_4.yml";

    // Assume plannerNames.length > 0
    private String makePlannersOptionString(Class<? extends TestExecutionPlanner>... planners) {
	List<String> pNames = new ArrayList<String>();
	Arrays.asList(planners).stream().sequential().map(Class::getName)
		.collect(Collectors.toCollection(() -> pNames));
	return makePlannersOptionString(pNames.toArray(new String[] {}));
    }

    private String makePlannersOptionString(String... plannerNames) {
	StringBuffer sb = new StringBuffer();
	sb.append("-p").append(" ");
	for (String plannerName : plannerNames) {
	    sb.append(plannerName).append(",");
	}
	// Remove trailing
	sb.deleteCharAt(sb.length() - 1);
	return sb.toString();
    }

    private final String defaultOptionsString = String.format("-t %s -i %s -b %s -c %s -g %s",
	    baseFolder + "/" + testJobs, //
	    baseFolder + "/" + availableImages, //
	    baseImage, //
	    baseFolder + "/" + cloudModel, //
	    baseFolder + "/" + goal);

    @Test
    public void testSequential() {
	try {
	    System.out.println(
		    String.format("%s %s", makePlannersOptionString(SequentialPlanner.class), defaultOptionsString));
	    EvaluationDriver driver = new EvaluationDriver();
	    driver.parseArgs(
		    String.format("%s %s", makePlannersOptionString(SequentialPlanner.class), defaultOptionsString)
			    .split(" "));
	    //
	    Result result = driver.execute();
	    System.out.println(result);
	} catch (Exception e) {
	    Assert.fail("Failed with exception", e);
	}

    }

    @Test
    public void testSequentialWithOSnap() {
	try {
	    EvaluationDriver driver = new EvaluationDriver();
	    driver.parseArgs(
		    String.format("%s %s", makePlannersOptionString(SequentialPlannerWithOpportunisticSnapshot.NAME),
			    defaultOptionsString).split(" "));
	    //
	    Result result = driver.execute();
	    System.out.println(result);
	} catch (Exception e) {
	    Assert.fail("Failed with exception", e);
	}
    }

    @Test
    public void testMaxParallelPlanner() {
	try {
	    EvaluationDriver driver = new EvaluationDriver();
	    driver.parseArgs(
		    String.format("%s %s", makePlannersOptionString(MaxParallelismPlanner.NAME), defaultOptionsString)
			    .split(" "));
	    //
	    Result result = driver.execute();
	    System.out.println(result);
	} catch (Exception e) {
	    Assert.fail("Failed with exception", e);
	}
    }

}
