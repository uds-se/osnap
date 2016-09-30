package de.unisaarland.cs.st.plot;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.unisaarland.cs.st.data.DataDriver;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Package;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.planners.MaxParallelismPlanner;
import de.unisaarland.cs.st.planners.SequentialPlanner;
import de.unisaarland.cs.st.util.SchedulePlotter;

public class SchedulePlotterTest {

    @Test
    public void plotEmptyScheduleTest() {
	Schedule s = Schedule.newEmptySchedule();
	try {
	    SchedulePlotter.plotSchedule(s);
	    Assert.fail("Expected exception not raised");

	} catch (Throwable e) {
	    // TODO Auto-generated catch block
	    // e.printStackTrace();
	}
    }

    @Test
    public void plotSequentialScheduleTest() {
	DataDriver driver = DataDriver.createTestData();
	System.out.println("TestExecutionPlanner.main()\n" + driver.printInputData());

	Image emptyImage = Image.getEmptyImage();
	// TODO Collect all the data from test jobs !
	Set<Package> allDeps = new HashSet<Package>();
	for (TestJob testJob : driver.testJobs) {
	    allDeps.addAll(testJob.getAllPackages());
	}

	Image fullImage = new Image();

	fullImage.installedPackages.addAll(allDeps);

	//
	SequentialPlanner planner = new SequentialPlanner();
	Schedule schedule = planner.computeSchedule(driver.testJobs, driver.availableImages, emptyImage,
		driver.cloudModel, driver.goal);
	System.out.println("SequentialPlanner.main() With Empty Image:\n" + schedule);

	File epsFile = null;
	try {
	    epsFile = SchedulePlotter.plotSchedule(schedule);
	    System.out.println("SchedulePlotterTest.plotSequentialScheduleTest() " + epsFile);
	} catch (Throwable e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    Assert.fail();
	}
	Assert.assertTrue(epsFile.exists());
    }

    @Test
    public void plotMaxParallelScheduleTest() {
	DataDriver driver = DataDriver.createTestData();
	System.out.println("TestExecutionPlanner.main()\n" + driver.printInputData());

	Image emptyImage = Image.getEmptyImage();
	// TODO Collect all the data from test jobs !
	Set<Package> allDeps = new HashSet<Package>();
	for (TestJob testJob : driver.testJobs) {
	    allDeps.addAll(testJob.getAllPackages());
	}

	Image fullImage = new Image();

	fullImage.installedPackages.addAll(allDeps);

	MaxParallelismPlanner planner = new MaxParallelismPlanner();
	Schedule schedule = planner.computeSchedule(driver.testJobs, driver.availableImages, emptyImage,
		driver.cloudModel, driver.goal);
	System.out.println("SequentialPlanner.main() With Empty Image:\n" + schedule);

	File epsFile = null;
	try {
	    epsFile = SchedulePlotter.plotSchedule(schedule);
	    System.out.println("SchedulePlotterTest.plotSequentialScheduleTest() " + epsFile);
	} catch (Throwable e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    Assert.fail();
	}
	Assert.assertTrue(epsFile.exists());
    }

    @Test
    public void plotManualScheduleTest() {
	Schedule schedule = new Schedule();
	Instance r1 = new Instance(1, true);
	List<Job> r1List = new ArrayList<Job>();
	Job j = new Job();
	j.id = 1;
	j.image = Image.getEmptyImage();
	j.snapshot = true;
	j.startTime = 0;
	j.endTime = 10;
	r1List.add(j);
	//
	j = new Job();
	j.id = 2;
	j.image = Image.getEmptyImage();
	j.snapshot = false;
	j.startTime = 10;
	j.endTime = 15;
	r1List.add(j);
	//
	schedule.jobsDistribution.put(r1, r1List);
	//
	Instance od1 = new Instance(1, false);
	List<Job> od1List = new ArrayList<Job>();
	j = new Job();
	j.id = 1;
	j.image = Image.getEmptyImage();
	j.snapshot = false;
	j.startTime = 0;
	j.endTime = 40;
	od1List.add(j);
	//
	schedule.jobsDistribution.put(od1, od1List);
	File epsFile = null;
	try {
	    epsFile = SchedulePlotter.plotSchedule(schedule);
	    System.out.println("SchedulePlotterTest.plotSequentialScheduleTest() " + epsFile);
	} catch (Throwable e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    Assert.fail();
	}
	Assert.assertTrue(epsFile.exists());
    }
}
