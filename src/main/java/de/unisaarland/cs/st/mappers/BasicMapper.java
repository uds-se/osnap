package de.unisaarland.cs.st.mappers;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.TestJob;

public class BasicMapper implements ITestJobToVMMapper {
    // This Mapper creates no additional snapshots
    private final List<Entry<Image, Image>> snapshots = new ArrayList<Entry<Image, Image>>();

    /**
     * Assumes that available images contains only 1 image, which is the base
     * image. If not, it simply picks the first one in the set. If the set is
     * empty or null it uses the emptyImage instead
     */
    @Override
    public Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> solve(Set<TestJob> testJobs,
	    Set<Image> availableImages, Image baseImage, CloudModel cloudModel, Goal goal) {
	
	Image _baseImage = (baseImage != null) ? baseImage : Image.getEmptyImage();

	Map<TestJob, Image> mapping = new HashMap<TestJob, Image>();
	for (TestJob testJob : testJobs)
	    mapping.put(testJob, _baseImage);
	return new AbstractMap.SimpleEntry<Map<TestJob, Image>, List<Entry<Image, Image>>>(mapping, this.snapshots);
    }
}
