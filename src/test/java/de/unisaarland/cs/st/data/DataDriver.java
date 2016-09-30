package de.unisaarland.cs.st.data;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

import de.unisaarland.cs.st.graph.OpportunisticSnapshotNetworkFlowModel;

/**
 * Data object class to store partial results from the different optimization
 * steps
 * 
 * MOVE TO TEST ?
 * 
 * @author alessiogambi
 *
 */
public class DataDriver {

    // Inputs:
    public Set<TestJob> testJobs;
    public Set<Image> availableImages;
    public CloudModel cloudModel;
    public Goal goal;
    //
    // public int availableProcessingUnits;
    // public int snapshottingCost;
    // public int snapshottingTime;
    //
    // public int snapshotBootTime;

    // First Optimization:
    // public Set<TestBatch> batches;

    // Second Optimization:
    // public Set<Image> snapshotsToCreate;
    // public Map<TestBatch, Image> batchesToImages;
    public long[] times = new long[4];

    public int initialCmax;
    public int initialCost;
    public int finalCost;
    public Schedule finalSchedule;
    public int finalCmax;
    // To speedup the evaluation
    public OpportunisticSnapshotNetworkFlowModel precomputedModel;
    // To account for evaluation speed up in the timing
    public long evaluationDelay;

    // 20160426_180000
    public static DataDriver createRealData() {
	return createRealData("test-jobs/debci/20160427_030000.yml");
    }

    public static DataDriver createRealData(String from) {
	String realDataBaseFolder = "src/main/resources/osdi2016/evaluation";
	String realDataTestJobs = from;

	String realDataCloudModel = "cloud-models/amazon-ec2-eu_1.yml";
	String readDataGoal = "goals/goal_80_20.yml";

	String realDataAvailableImages = "images/debian-8.yml";
	// Note this one
	DataDriver driver = DataDriver.createTestData();

	YamlReader reader;
	try {
	    reader = new YamlReader(new FileReader(realDataBaseFolder + "/" + readDataGoal));
	    driver.goal = reader.read(Goal.class);
	    reader.close();
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (YamlException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	try {
	    reader = new YamlReader(new FileReader(realDataBaseFolder + "/" + realDataTestJobs));
	    driver.testJobs = reader.read(Set.class, TestJob.class);

	    // TODO: THIS IS ONLY FOR TESTING !
	    for (TestJob tj : driver.testJobs) {
		if (tj.getTotalSetupTime() == 0) {
		    for (Package p : tj.sut.dependencies) {
			p.downloadTime = 1000;
			p.installationTime = 1000;
		    }
		    tj.sut.installationTime = 5000;
		}
	    }

	    reader.close();
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (YamlException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	try {
	    reader = new YamlReader(new FileReader(realDataBaseFolder + "/" + realDataCloudModel));
	    driver.cloudModel = reader.read(CloudModel.class);
	    reader.close();
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (YamlException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	try {
	    reader = new YamlReader(new FileReader(realDataBaseFolder + "/" + realDataAvailableImages));
	    driver.availableImages = reader.read(HashSet.class, Image.class);
	    reader.close();
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (YamlException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return driver;
    }

    public static DataDriver createInitialTestData() {
	Package pB = new Package();
	pB.installationTime = 20;
	pB.name = "B";

	Package pA = new Package();
	pA.installationTime = 1;
	pA.name = "A";
	pA.dependencies.add(pB);

	TestJob tA = new TestJob();
	tA.sut = pA;
	tA.testDuration = 10;

	TestJob tB = new TestJob();
	tB.sut = pB;
	tB.testDuration = 4;

	Set<TestJob> tests = new HashSet<TestJob>();
	tests.add(tA);
	tests.add(tB);

	Set<Image> availableImages = new HashSet<Image>();
	// By defatul - empty image - this might change for distributions that
	// have a number core pre-installed packages
	Image baseImage = new Image();
	baseImage.name = "Empty";
	availableImages.add(baseImage);

	DataDriver d = new DataDriver();
	d.testJobs = tests;
	d.availableImages = availableImages;

	// This defines available instances, BUT, and so forth
	d.cloudModel = new CloudModel(60, 0, 10, 1, -1, -1, 20);

	//
	d.goal = Goal.getMinCostGoal();
	d.goal.maxTime = 120;
	//
	return d;

    }

    public static DataDriver createTestData() {
	// We compute the ideal/optimal set of snapshots that minimize the total
	// costs of running the test suite
	// NOTE: This might not be the one that minimize the Cmax !!!
	Package pB = new Package();
	pB.installationTime = 5;
	pB.name = "B";
	pB.version = "1";

	Package pA = new Package();
	pA.installationTime = 10;
	pA.name = "A";
	pA.version = "1.0";
	pA.dependencies.add(pB);

	TestJob tA = new TestJob();
	tA.sut = pA;
	tA.testDuration = 12;

	TestJob tB = new TestJob();
	tB.sut = pB;
	tB.testDuration = 9;

	Set<TestJob> tests = new HashSet<TestJob>();
	tests.add(tA);
	tests.add(tB);

	// Snapshots
	Set<Image> availableImages = new HashSet<Image>();
	// TODO This is somehow included in the model
	// Image baseImage = new Image();
	// baseImage.name = "Empty";
	availableImages.add(Image.getEmptyImage());

	// Image A = new Image();
	// A.installedPackages.add(pA);
	// A.parentImage = baseImage;
	// A.name = "A";
	// availableImages.add(A);

	DataDriver d = new DataDriver();
	d.testJobs = tests;
	d.availableImages = availableImages;
	// This defines available instances, BUT, and so forth
	d.cloudModel = new CloudModel(60, 0, 5, 1, -1, -1, 20);

	//
	d.goal = new Goal(0.8, 0.2);
	d.goal.maxOnDemandInstances = 4;
	d.goal.maxCost = 10;
	//
	return d;
    }

    public static DataDriver createAnotherTestData() {
	Set<TestJob> testJobs = new HashSet<TestJob>();

	Package pB = new Package();
	pB.installationTime = 5;
	pB.name = "B";

	Package pA = new Package();
	pA.installationTime = 10;
	pA.name = "A";
	pA.dependencies.add(pB);

	// TODO Probably a Factory method
	TestJob t1 = new TestJob();
	t1.sut = pA;
	t1.testDuration = 10;

	TestJob t2 = new TestJob();
	t2.sut = pB;
	t2.testDuration = 13;

	testJobs.add(t1);
	testJobs.add(t2);

	// Snapshots
	// TODO Probably a Factory method
	Set<Image> images = new HashSet<Image>();

	Image image = new Image();
	image.name = "EMPTY";
	images.add(image);

	// image = new Image();
	// image.installedPackages.add(pB);
	// images.add(image);

	DataDriver d = new DataDriver();
	d.testJobs = testJobs;
	d.availableImages = images;
	// This defines available instances, BUT, and so forth
	d.cloudModel = new CloudModel(60, 0, 20, 4, -1, -1, 1);
	//
	//
	d.goal = Goal.getMinTimeGoal();
	//
	return d;
    }

    // TODO Move to another component in between OSnap and FinalScheduling
    // public String printNewSnapshots() {
    // StringBuffer sb = new StringBuffer();
    // // Compute new snapshots if any
    // Set<Image> newSnapshots = new
    // HashSet<Image>(this.batchesToImages.values());
    // newSnapshots.removeAll(this.availableImages);
    // newSnapshots.toString();
    //
    // sb.append("New Snapshots: \n");
    // for (Image image : newSnapshots) {
    // sb.append(String.format("%10s [ from %5s ]", image.getId(),
    // image.parentImage)).append("\n");
    // }
    // return sb.toString();
    // }

    // TODO Move to another component in between OSnap and FinalScheduling
    // public String printNewSnapshotsFull() {
    // StringBuffer sb = new StringBuffer();
    // // Compute new snapshots if any
    // Set<Image> newSnapshots = new
    // HashSet<Image>(this.batchesToImages.values());
    // newSnapshots.removeAll(this.availableImages);
    // newSnapshots.toString();
    //
    // sb.append("New Snapshots: \n");
    // for (Image image : newSnapshots) {
    // sb.append(String.format("%10s [ from %5s ]", image.getId(),
    // image.parentImage)).append("\n");
    // sb.append(image.getPartialInstallationCost());
    // sb.append(image.getTotalInstallationCost());
    // sb.append(image.installedPackages).append("\n");
    //
    // }
    // return sb.toString();
    // }

    public String printInputData() {
	StringBuffer sb = new StringBuffer();

	sb.append("Available Images: \n");
	for (Image image : this.availableImages) {
	    sb.append(String.format("%10s", image.getLabel())).append(
		    (image.parentImage != null) ? String.format(" [ from %5s ]", image.parentImage.getLabel()) : "")
		    .append("\n");
	}

	sb.append("Test Jobs: \n");
	for (TestJob test : this.testJobs) {
	    sb.append(String.format("\t%s %10s %5s %s", test.id, test.sut, test.testDuration, test.getAllPackages()))
		    .append("\n");
	}

	sb.append("Cloud Model: \n");
	sb.append(String.format("%s", cloudModel)).append("\n");

	sb.append("Goal: \n");
	sb.append(String.format("%s", goal)).append("\n");
	return sb.toString();
    }

    public String printOutputData() {
	StringBuffer sb = new StringBuffer();
	sb.append(String.format("Cmax: %d\nInitial Cmax: %d\nImprovement on Cmax: %2.2f %s\n", this.finalCmax,
		this.initialCmax,
		(((double) this.initialCmax - (double) this.finalCmax) / (double) this.initialCmax) * 100, "%"))
		.append(String.format("Cost: %d\nInitial Cost: %d\nImprovement on Cost: %2.2f %s\n", this.finalCost,
			this.initialCost,
			(((double) this.initialCost - (double) this.finalCost) / (double) this.initialCost) * 100, "%"))
		.append(String.format("Time to compute: %d\n", (this.times[3] - this.times[0])))
		.append(String.format("Time Breakdown: %d, %d, %d\n", (this.times[1] - this.times[0]),
			(this.times[2] - this.times[1]), (this.times[3] - this.times[2])));
	return sb.toString();
    }

    public String printSchedule() {
	StringBuffer sb = new StringBuffer();
	sb.append("Schedule: \n" + this.finalSchedule);
	return sb.toString();
    }

    public String prettyPrint() {
	StringBuffer sb = new StringBuffer();
	sb.append(printInputData()).append("\n").append(printOutputData()).append("\n").append(printSchedule());
	return sb.toString();

    }
}
