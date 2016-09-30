package de.unisaarland.cs.st.planners;

import de.unisaarland.cs.st.mappers.CachedOpportunisticSnapshot;
import de.unisaarland.cs.st.schedulers.ILPScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;

public class ILPPlannerWithOpportunisticSnapshot extends TestExecutionPlanner {

    public ILPPlannerWithOpportunisticSnapshot() {
	super("ILPPlannerWithOpportunisticSnapshot", new CachedOpportunisticSnapshot(), new ILPScheduler(),
		new BasicInputDataValidator());
    }
}
