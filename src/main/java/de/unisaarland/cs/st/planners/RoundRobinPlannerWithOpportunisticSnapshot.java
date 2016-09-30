package de.unisaarland.cs.st.planners;

import de.unisaarland.cs.st.mappers.CachedOpportunisticSnapshot;
import de.unisaarland.cs.st.schedulers.RoundRobinScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;

public class RoundRobinPlannerWithOpportunisticSnapshot extends TestExecutionPlanner {

    public RoundRobinPlannerWithOpportunisticSnapshot() {
	super("RoundRobinPlannerWithOpportunisticSnapshot", new CachedOpportunisticSnapshot(),
		new RoundRobinScheduler(), new BasicInputDataValidator());
    }

}
