package de.unisaarland.cs.st.planners;

import de.unisaarland.cs.st.mappers.BasicMapper;
import de.unisaarland.cs.st.schedulers.RoundRobinScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;

public class RoundRobinPlanner extends TestExecutionPlanner {

    public RoundRobinPlanner() {
	super("RoundRobinPlanner", new BasicMapper(), new RoundRobinScheduler(), new BasicInputDataValidator());
    }

}
