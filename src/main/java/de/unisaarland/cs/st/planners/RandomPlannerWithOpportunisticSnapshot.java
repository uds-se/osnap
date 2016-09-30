package de.unisaarland.cs.st.planners;

import de.unisaarland.cs.st.mappers.CachedOpportunisticSnapshot;
import de.unisaarland.cs.st.schedulers.RandomScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;

public class RandomPlannerWithOpportunisticSnapshot extends TestExecutionPlanner {

    public RandomPlannerWithOpportunisticSnapshot() {
	super("RandomPlannerWithOpportunisticSnapshot", new CachedOpportunisticSnapshot(), new RandomScheduler(),
		new BasicInputDataValidator());
    }

}
