package de.unisaarland.cs.st.planners;

import de.unisaarland.cs.st.mappers.CachedOpportunisticSnapshot;
import de.unisaarland.cs.st.schedulers.MinLoadScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;

public class MinLoadPlannerWithOpportunisticSnapshot extends TestExecutionPlanner {

    public static final String NAME = "MinLoadPlannerWithOpportunisticSnapshot";

    public MinLoadPlannerWithOpportunisticSnapshot() {
	super(NAME, new CachedOpportunisticSnapshot(), new MinLoadScheduler(), new BasicInputDataValidator());

    }

}
