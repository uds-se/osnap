package de.unisaarland.cs.st.mappers;

import java.io.FileNotFoundException;

import org.apache.commons.cli.ParseException;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;

import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.evaluation.EvaluationDriver;
import de.unisaarland.cs.st.evaluation.Result;

public class TestWithPackageUpdates {

    final static String OPT = "--stop-on-error --planners %s --test-jobs %s --available-images %s --base-image-id %s --cloud-model %s --goal %s";

    @Test
    public void mainTestWithPackageUpdates() throws FileNotFoundException, YamlException, ParseException {
	//
	String planners = "de.unisaarland.cs.st.planners.SequentialPlannerWithFastOpportunisticSnapshot";

	// This is the second round after creating the snapshots
	String testJobs = "src/test/resources/update/test-jobs_gnuplot_1443858262.yml";

	// This shall contain at least one snapshot
	// Sat Oct 3 09:44:22 CEST 2015
	String availableImages = "src/test/resources/update/available-images_20151003T094422Z.yml";

	String baseImageId = "1443685462";

	String cloudModel = "src/test/resources/update/cloud-model.yml";
	String goal = "src/test/resources/update/goal.yml";
	//
	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
	Result result = driver.execute();
	for (Schedule schedule : result.getSchedules().values()) {
	    System.err.println(schedule);
	}
    }

    @Test
    public void mainTestOSnapWithPackageUpdates() throws FileNotFoundException, YamlException, ParseException {
	String planners = "de.unisaarland.cs.st.planners.MinLoadPlannerWithOpportunisticSnapshot";

	// This is the second round after creating the snapshots
	String testJobs = "src/test/resources/update-osnap/test-jobs_gnuplot_1443685462.yml";

	// This shall contain at least one snapshot
	// Sat Oct 3 09:44:22 CEST 2015
	String availableImages = "src/test/resources/update-osnap/available-images_20151001T094422Z.yml";

	String baseImageId = "1443685462";

	String cloudModel = "src/test/resources/update-osnap/cloud-model.yml";
	String goal = "src/test/resources/update-osnap/goal.yml";
	//
	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
	Result result = driver.execute();
	for (Schedule schedule : result.getSchedules().values()) {
	    System.err.println(schedule);
	}
    }

    @Test
    public void mainTestOSnapWithPackageUpdatesCyclicDep() throws FileNotFoundException, YamlException, ParseException {
	String planners = "de.unisaarland.cs.st.planners.MinLoadPlannerWithOpportunisticSnapshot";

	// This is the second round after creating the snapshots
	String testJobs = "src/test/resources/update-osnap/test-jobs_gnuplot_1445762662.yml";

	// This shall contain at least one snapshot
	// Sat Oct 3 09:44:22 CEST 2015
	String availableImages = "src/test/resources/update-osnap/available-images_20151025T094422Z.yml";

	String baseImageId = "1443685462";

	String cloudModel = "src/test/resources/update-osnap/cloud-model.yml";
	String goal = "src/test/resources/update-osnap/goal.yml";
	//
	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
	Result result = driver.execute();
	for (Schedule schedule : result.getSchedules().values()) {
	    System.err.println(schedule);
	}
    }

    @Test
    public void mainTestFastOSnapWithPackageUpdatesCyclicDep()
	    throws FileNotFoundException, YamlException, ParseException {
	String planners = "de.unisaarland.cs.st.planners.MinLoadPlannerWithFastOpportunisticSnapshot";

	// This is the second round after creating the snapshots
	String testJobs = "src/test/resources/update-osnap/test-jobs_gnuplot_1445762662.yml";

	// This shall contain at least one snapshot
	// Sat Oct 3 09:44:22 CEST 2015
	String availableImages = "src/test/resources/update-osnap/available-images_20151025T094422Z.yml";

	String baseImageId = "1443685462";

	String cloudModel = "src/test/resources/update-osnap/cloud-model.yml";
	String goal = "src/test/resources/update-osnap/goal.yml";
	//
	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
	Result result = driver.execute();
	for (Schedule schedule : result.getSchedules().values()) {
	    System.err.println(schedule);
	}
    }

    @Test
    public void mainTestOSnapHugePenalty() throws FileNotFoundException, YamlException, ParseException {
	String planners = "de.unisaarland.cs.st.planners.MinLoadPlannerWithOpportunisticSnapshot,de.unisaarland.cs.st.planners.MinLoadPlannerWithFastOpportunisticSnapshot";
	// This is the second round after creating the snapshots
	String testJobs = "src/test/resources/update-osnap/test-jobs_gnuplot_1444203862.yml";

	// This shall contain at least one snapshot
	// Sat Oct 3 09:44:22 CEST 2015
	String availableImages = "src/test/resources/update-osnap/available-images_20151007T094422Z.yml";

	String baseImageId = "1443685462";

	String cloudModel = "src/test/resources/update-osnap/cloud-model.yml";
	String goal = "src/test/resources/update-osnap/goal.yml";
	//
	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(
		String.format(OPT, planners, testJobs, availableImages, baseImageId, cloudModel, goal).split(" "));
	Result result = driver.execute();
	for (Schedule schedule : result.getSchedules().values()) {
	    System.err.println(schedule);
	}
    }
}
