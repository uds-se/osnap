package de.unisaarland.cs.st.mappers;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.TestJob;

/**
 * This interface must be implemented by the first component in the chain, which
 * takes TestJobs and available Images as input and returns a mapping of Test
 * Jobs to available or next-to-be created images;
 * 
 * @author gambi
 *
 */
public interface ITestJobToVMMapper {

    /**
     * Returns a maps which specifies on which image test jobs will run, and a
     * list of snapshots to create defined as mapping between a parent image and
     * a child image
     * 
     * @return
     */
    public Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> solve(
	    // All input data
	    Set<TestJob> testJobs,
	    Set<Image> availableImages,
	    Image baseImage,
	    CloudModel cloudModel, Goal goal);
}
