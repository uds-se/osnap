package de.unisaarland.cs.st.mappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.TestJob;

/**
 * This class contains an IN-MEMORY Cache ! Which is a static final field of
 * this class so all the CachedOpportunisticSnapshot are linked to it !
 * 
 * @author gambi
 *
 */
public class CachedOpportunisticSnapshot extends CachedMapper {

    private OpportunisticSnapshot internalMapper = new OpportunisticSnapshot();
    // TODO For the moment we just use abstract class Object as key because I am
    // not sure about TestJob.
    // FIXME Synch here ? We do not actually plan to parallelize this anywya
    private final static Map<String, Entry<Map<TestJob, Image>, List<Entry<Image, Image>>>> inMemoryCache = new HashMap<String, Entry<Map<TestJob, Image>, List<Entry<Image, Image>>>>();
    private final static Map<String, Long> inMemoryCacheComputationTime = new HashMap<String, Long>();

    // Mostly for testing
    void clearCache() {
	inMemoryCache.clear();
	inMemoryCacheComputationTime.clear();
    }

    @Override
    public long getComputationTime(Set<TestJob> testJobs, Set<Image> availableImages, Image baseImage,
	    CloudModel cloudModel, Goal goal) {
	String index = computeIndex(testJobs, availableImages, baseImage, cloudModel, goal);
	if (inMemoryCacheComputationTime.containsKey(index))
	    return inMemoryCacheComputationTime.get(index);
	else
	    return -1;
    }

    /**
     * Compute and store the value. Probably store should be implemente on its
     * own ?
     */
    @Override
    public Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> compute(Set<TestJob> testJobs,
	    Set<Image> availableImages, Image baseImage, CloudModel cloudModel, Goal goal) {
	//
	long startComputation = System.currentTimeMillis();
	Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> mapping = internalMapper.solve(testJobs, availableImages,
		baseImage, cloudModel, goal);
	long endComputation = System.currentTimeMillis();
	// Store in cache
	String index = computeIndex(testJobs, availableImages, baseImage, cloudModel, goal);
	inMemoryCache.put(index, mapping);
	inMemoryCacheComputationTime.put(index, (endComputation - startComputation));
	// Return mapping
	return mapping;
    }

    @Override
    public Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> getFromCache(Set<TestJob> testJobs,
	    Set<Image> availableImages, Image baseImage, CloudModel cloudModel, Goal goal) {
	return inMemoryCache.get(computeIndex(testJobs, availableImages, baseImage, cloudModel, goal));
    }

    @Override
    public boolean cacheContains(Set<TestJob> testJobs, Set<Image> availableImages, Image baseImage,
	    CloudModel cloudModel, Goal goal) {
	return inMemoryCache.containsKey(computeIndex(testJobs, availableImages, baseImage, cloudModel, goal));
    }

    private String computeIndex(Set<TestJob> testJobs, Set<Image> availableImages, Image baseImage,
	    CloudModel cloudModel, Goal goal) {
	// Since all the objects implements their equals and hashCode it might
	// be fine just to invoke equals to them !
	// Goal and Set<Images> and Images, Cloud Model. Object its just a int[]
	// composition of their hashCodes !
	String index = testJobs.hashCode() + "++" + availableImages.hashCode() + "++" + baseImage.hashCode() + "++"
		+ cloudModel.hashCode() + "++" + cloudModel.hashCode() + "++" + goal.hashCode();

	return index;
    }

}
