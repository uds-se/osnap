package de.unisaarland.cs.st.planners;

import de.unisaarland.cs.st.mappers.BasicMapper;
import de.unisaarland.cs.st.schedulers.RandomScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;

public class RandomPlanner extends TestExecutionPlanner {

    public RandomPlanner() {
	super("RandomPlanner", new BasicMapper(), new RandomScheduler(), new BasicInputDataValidator());
    }

}
