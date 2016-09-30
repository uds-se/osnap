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
 * This mapper returns cached result instead of computing them.
 * 
 * @author gambi
 *
 */
public abstract class CachedMapper implements ITestJobToVMMapper {

    @Override
    public Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> solve(Set<TestJob> testJobs,
	    Set<Image> availableImages, Image baseImage, CloudModel cloudModel, Goal goal) {

	return cacheContains(testJobs, availableImages, baseImage, cloudModel, goal)
		? getFromCache(testJobs, availableImages, baseImage, cloudModel, goal)
		: compute(testJobs, availableImages, baseImage, cloudModel, goal);
    }

    public abstract Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> compute(Set<TestJob> testJobs,
	    Set<Image> availableImages, Image baseImage, CloudModel cloudModel, Goal goal);

    public abstract Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> getFromCache(Set<TestJob> testJobs,
	    Set<Image> availableImages, Image baseImage, CloudModel cloudModel, Goal goal);

    public abstract boolean cacheContains(Set<TestJob> testJobs, Set<Image> availableImages, Image baseImage,
	    CloudModel cloudModel, Goal goal);

    /**
     * This return the original time to compute the cached solution
     * 
     * @param _testJobs
     * @param availableImages
     * @param baseImage
     * @param cloudModel
     * @param goal
     * @return
     */
    public abstract long getComputationTime(Set<TestJob> _testJobs, Set<Image> availableImages, Image baseImage,
	    CloudModel cloudModel, Goal goal);

}
