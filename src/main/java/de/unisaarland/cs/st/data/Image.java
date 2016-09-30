package de.unisaarland.cs.st.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

// FIXME Why there is no DOWNLOAD TIME AS WELL !?
public class Image implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -1031621512760290827L;
    private static final AtomicInteger ID_FACTORY = new AtomicInteger();

    private static Image EMPTY_IMAGE;

    private /* final */ String id;

    public Set<Package> installedPackages;
    public String name;
    public Image parentImage;// in case the snapshot is derived !

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((installedPackages == null) ? 0 : installedPackages.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Image other = (Image) obj;
	if (installedPackages == null) {
	    if (other.installedPackages != null)
		return false;
	} else if (!installedPackages.equals(other.installedPackages))
	    return false;
	return true;
    }

    public Image() {
	id = "" + ID_FACTORY.incrementAndGet();
	installedPackages = new HashSet<Package>();
    }

    // public Image(String id) {
    // this.id = id;
    // installedPackages = new HashSet<Package>();
    // }

    // Utility method

    // public int getTotalInstallationCost() {
    // int installationCost = 0;
    // for (Package p : installedPackages) {
    // installationCost = installationCost + p.getInstallationCost();
    // }
    // return installationCost;
    // }

    public long getSetupTimeWithImageMillisec(Image parentImage) {
	long totalSetupTimeMillisec = 0;

	Set<Package> missingDependencies = new HashSet<Package>();
	// Collect required deps to build this one
	missingDependencies.addAll(installedPackages);
	// Remove the ones already installed in the parent
	missingDependencies.removeAll(parentImage.installedPackages);

	for (Package p : missingDependencies) { // Install all the missing deps
	    totalSetupTimeMillisec = totalSetupTimeMillisec + p.installationTime + p.downloadTime;
	}
	return totalSetupTimeMillisec;
    }

    /**
     * This might return 0 if the setup time in msec is smaller than 0.5
     * 
     * @param parentImage
     * @return
     */
    public long getSetupTimeWithImage(Image parentImage) {
	// TimeUnit.MILLISECONDS.toSeconds(totalSetupTimeMillisec);
	// totalSetupTimeMillisec / 1000
	long msec = getSetupTimeWithImageMillisec(parentImage);
	if (msec >= Double.MAX_VALUE) {
	    return msec / 1000;
	} else {
	    return Math.round(((double) msec) / 1000);
	}
    }

    // int --> long rounding make the values different !
    /**
     * DO NOT USE THIS TO COMPUTE DIFFERENCES, USE getSetupTimeWithImage(Image
     * parent) instead
     * 
     * @return
     */
    public long getTotalInstallationTimeMillisec() {
	long installationTime = 0;
	for (Package p : installedPackages) {
	    installationTime = installationTime + p.installationTime + p.downloadTime;// getInstallationTimeSeconds();
	}
	return installationTime;
    }

    // Round up to one if too small !
    public long getTotalInstallationTime() {
	long msec = getTotalInstallationTimeMillisec();
	if (msec >= Double.MAX_VALUE) {
	    return msec / 1000;
	} else {
	    //
	    if (msec > 0 && Math.round(((double) msec) / 1000) == 0)
		return 1;
	    else
		return Math.round(((double) msec) / 1000);
	}
    }

    /**
     * This is basically the cost of creating a snapshot from another VM by
     * computing the DELTAS of dependencies
     * 
     * Basic intepretation: if the package is missing it gets installed, no
     * unistall of anything else for the moment
     * 
     * @return
     */
    // public int getPartialInstallationCost() {
    // int installationCost = 0;
    //
    // if (parentImage == null || parentImage.installedPackages.isEmpty()) {
    // return getTotalInstallationCost();
    // }
    //
    // for (Package p : installedPackages) {
    // if (!parentImage.installedPackages.contains(p)) {
    // installationCost = installationCost + p.getInstallationCost();
    // }
    // }
    // return installationCost;
    // }

    public long getPartialInstallationTime() {
	long installationTime = 0;

	if (parentImage == null || parentImage.installedPackages.isEmpty()) {
	    return getTotalInstallationTime();
	}

	for (Package p : installedPackages) {
	    if (!parentImage.installedPackages.contains(p)) {
		installationTime = installationTime + p.installationTime + p.downloadTime;// getInstallationTimeSeconds();
	    }
	}
	return Math.round(((double) installationTime) / 1000);
    }

    @Override
    public String toString() {
	return ((name == null) ? id : name);
	// return "Image Node\n" + ((name != null) ? name
	// : (installedPackages.size() > 2) ? id :
	// Arrays.toString(installedPackages.toArray()));
    }

    public String getId() {
	return id;
    }

    public String getLabel() {
	return "ImageNode_" + id + ((name != null) ? " " + name : "");
    }

    public static Image getEmptyImage() {
	if (EMPTY_IMAGE == null) {
	    EMPTY_IMAGE = new Image();
	    EMPTY_IMAGE.name = "Empty";
	}
	return EMPTY_IMAGE;
    }

    public String prettyPrint() {
	return getLabel() + "[" + this.installedPackages + "]";
    }

    public void update(Image snapshotImage) {
	snapshotImage.id = this.id;
	snapshotImage.installedPackages = this.installedPackages;
	snapshotImage.parentImage = this.parentImage;
	snapshotImage.name = this.name;
    }
}
