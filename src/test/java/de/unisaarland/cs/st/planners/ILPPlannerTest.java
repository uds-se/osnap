package de.unisaarland.cs.st.planners;

import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import de.unisaarland.cs.st.data.DataDriver;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Package;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.data.TestJob;

public class ILPPlannerTest {

    @Test
    public void ilpPlannerTest() {

	DataDriver driver = DataDriver.createTestData();
	System.out.println("ILPPlanner.main()\n" + driver.printInputData());

	Image emptyImage = Image.getEmptyImage();
	// TODO Collect all the data from test jobs !
	Set<Package> allDeps = new HashSet<Package>();
	for (TestJob testJob : driver.testJobs) {
	    allDeps.addAll(testJob.getAllPackages());
	}

	Image fullImage = new Image();
	fullImage.installedPackages.addAll(allDeps);
	fullImage.name = "Full";

	//
	ILPPlanner planner = new ILPPlanner();
	Schedule schedule = planner.computeSchedule(driver.testJobs, driver.availableImages, emptyImage,
		driver.cloudModel, driver.goal);

	System.out.println("ILPPlanner.main() With Empty Image:\n" + schedule);

	planner = new ILPPlanner();
	schedule = planner.computeSchedule(driver.testJobs, driver.availableImages, fullImage, driver.cloudModel,
		driver.goal);

	System.out.println("ILPPlanner.main() With Full Image :\n" + schedule);

    }

    @Test
    public void ilpPlannerWithOSnapTest() {
	DataDriver driver = DataDriver.createTestData();
	System.out.println("ILPPlanner.main()\n" + driver.printInputData());

	Image emptyImage = Image.getEmptyImage();
	// TODO Collect all the data from test jobs !
	Set<Package> allDeps = new HashSet<Package>();
	for (TestJob testJob : driver.testJobs) {
	    allDeps.addAll(testJob.getAllPackages());
	}

	Image fullImage = new Image();
	fullImage.installedPackages.addAll(allDeps);
	fullImage.name = "Full";

	//
	ILPPlannerWithOpportunisticSnapshot planner = new ILPPlannerWithOpportunisticSnapshot();
	Schedule schedule = planner.computeSchedule(driver.testJobs, driver.availableImages, emptyImage,
		driver.cloudModel, driver.goal);

	System.out.println("ILPPlannerWithOpportunisticSnapshot.main() With Empty Image:\n" + schedule);

	planner = new ILPPlannerWithOpportunisticSnapshot();
	// driver.availableImages.add( fullImage);
	// NOTE: when baseImage is not part of the available images, like here,
	// it will be snapshotted as all the testJobs are supposed to use it as
	// implicit setup !
	//
	schedule = planner.computeSchedule(driver.testJobs, driver.availableImages, fullImage, driver.cloudModel,
		driver.goal);

	System.out.println("ILPPlannerWithOpportunisticSnapshot.main() With Full Image :\n" + schedule);
    }

}
