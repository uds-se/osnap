package de.unisaarland.cs.st.util;

import java.util.Set;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.TestJob;

public class BasicInputDataValidator implements IInputDataValidator {

    // TODO add validation on BaseImage
    public void validateInputData(Set<TestJob> testJobs, Set<Image> availableImages, Image baseImage,
	    CloudModel cloudModel, Goal goal) {

	// No test jobs default to emptySchedule
	if (testJobs == null) {
	    throw new IllegalArgumentException("Test Jobs cannot be null or empty");
	}

	if (cloudModel == null) {
	    throw new IllegalArgumentException("Cloud model cannot be null or empty");
	}

	if (goal == null) {
	    throw new IllegalArgumentException("Goal cannot be null or empty");
	}
    }
}
