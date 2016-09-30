package de.unisaarland.cs.st.data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TestJob implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6343111437226835690L;

    public int id;

    public Date runWhen;

    // Optional: test snapshot used to run this test
    public Image runWith;

    private static AtomicInteger idFactory = new AtomicInteger();

    public Package sut;

    // Test Dependencies
    // public Set<Package> dependencies;

    // Sec ?!
    public long submissionTime;
    public long testDuration;// excluded dependency installation

    // public int getTestDurationSeconds() {
    // return testDuration / 1000;
    // }

    public TestJob() {
	// dependencies = new HashSet<Package>();
	id = idFactory.incrementAndGet();
    }

    // Deep Copy
    private TestJob(int id) {
	this.id = id;
    }

    /**
     * This will not increment the IDs, SUT/Package is copied by ref anyway !!
     * 
     * @param orig
     * @return
     */
    public static TestJob deepCopy(TestJob orig) {
	TestJob copy = new TestJob(orig.id);
	copy.runWhen = orig.runWhen;
	copy.runWith = orig.runWith;
	copy.submissionTime = orig.submissionTime;
	copy.testDuration = orig.testDuration;
	// TODO What about this one ?! Do we need to deep-copy it as well ?!
	copy.sut = orig.sut;
	return copy;
    }

    public long getSetupTimeWithImage(Image image) {
	long msec = getSetupTimeWithImageMillisec(image);
	if (msec >= Double.MAX_VALUE) {
	    return msec / 1000;
	} else {
	    return Math.round(((double) msec) / 1000);
	}
    }

    public long getSetupTimeWithImageMillisec(Image image) {

	if (image == null)
	    return -1;

	long totalSetupTime = 0;

	Set<Package> missingDependencies = new HashSet<Package>();
	// Collect required
	// missingDependencies.addAll(dependencies);
	missingDependencies.addAll(sut.dependencies);
	// Remove the ones already installed
	missingDependencies.removeAll(image.installedPackages);
	// Install SUT/Code
	totalSetupTime = 0;
	for (Package p : missingDependencies) { // Install all the missing deps
	    totalSetupTime = totalSetupTime + p.installationTime + p.downloadTime;
	}
	return totalSetupTime;
    }

    public int getDownloadTimeWithImage(Image image) {
	int totalSetupTime = 0;

	Set<Package> missingDependencies = new HashSet<Package>();
	// Collect required
	// missingDependencies.addAll(dependencies);
	missingDependencies.addAll(sut.dependencies);
	// Remove the ones already installed
	missingDependencies.removeAll(image.installedPackages);
	// Install SUT/Code
	totalSetupTime = 0;
	for (Package p : missingDependencies) { // Install all the missing deps
	    totalSetupTime = totalSetupTime + p.downloadTime;
	}
	return totalSetupTime / 1000;
    }

    public int getInstallationTimeWithImage(Image image) {
	int totalSetupTime = 0;

	Set<Package> missingDependencies = new HashSet<Package>();
	// Collect required
	// missingDependencies.addAll(dependencies);
	missingDependencies.addAll(sut.dependencies);
	// Remove the ones already installed
	missingDependencies.removeAll(image.installedPackages);
	// Install SUT/Code
	totalSetupTime = 0;
	for (Package p : missingDependencies) { // Install all the missing deps
	    totalSetupTime = totalSetupTime + p.installationTime;
	}
	return totalSetupTime / 1000;
    }

    /**
     * Total setup time assuming empty VM
     * 
     * @return
     */
    // Use getTimeWithImage instead
    public int getTotalSetupTime() {
	int totalSetupTime = 0;
	for (Package p : sut.dependencies) {
	    totalSetupTime = totalSetupTime + p.installationTime + p.downloadTime;
	}
	return totalSetupTime / 1000;
    }

    @Override
    public String toString() {
	return "T_" + id + "-" + sut.toString();
	// + "(" + (testDuration / 1000) + " (s) + "
	// + ((int) Math.ceil((double) sut.installationTime / 1000)) + " (s) +"
	// // getSourceInstallationTimeSeconds()
	// + (getTotalSetupTime() / 1000) + " (s)) " + ((this.runWhen != null) ?
	// "Run When : " + this.runWhen : "")
	// + "<-- " + getAllPackages() + "";
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	TestJob other = (TestJob) obj;

	// if (dependencies == null) {
	// if (other.dependencies != null)
	// return false;
	// } else if (!dependencies.equals(other.dependencies))
	// return false;

	if (runWhen == null) {
	    if (other.runWhen != null)
		return false;
	} else if (!runWhen.equals(other.runWhen))
	    return false;

	if (sut == null) {
	    if (other.sut != null)
		return false;
	} else if (!sut.equals(other.sut)) {
	    if (sut.dependencies == null) {
		if (other.sut.dependencies != null)
		    return false;
	    } else if (!sut.dependencies.equals(other.sut.dependencies))
		return false;
	    return false;
	}

	if (testDuration != other.testDuration)
	    return false;
	return true;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((runWhen == null) ? 0 : runWhen.hashCode());
	result = prime * result + (int) (submissionTime ^ (submissionTime >>> 32));
	result = prime * result + ((sut == null) ? 0 : sut.hashCode());
	result = prime * result + (int) (testDuration ^ (testDuration >>> 32));
	return result;
    }

    /**
     * @return
     */
    public Set<Package> getAllPackages() {
	Set<Package> totalDeps = new HashSet<Package>();
	// totalDeps.addAll(dependencies);
	totalDeps.addAll(sut.dependencies);
	// SUT is already included in sut.dependencies. which btw includes also
	// all the binaries it provides
	// totalDeps.add(sut); -> If we add this the ref from the test job is
	// different
	return totalDeps;
    }

}
