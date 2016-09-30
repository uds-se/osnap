package de.unisaarland.cs.st.planners;

import de.unisaarland.cs.st.mappers.BasicMapper;
import de.unisaarland.cs.st.schedulers.MaxParallelismScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;

/**
 * 
 * This Test Execution Planner implements the maximum parallelism planner that
 * is 1 test job for 1 instance (regarless instances are reseved or not!)
 * 
 * @author gambi
 *
 */
// TODO Account for Snapshots as well !
public class MaxParallelismPlannerOnlyOnDemand extends TestExecutionPlanner {

    public static final String NAME = "MaxParallelismPlannerOnlyOnDemand";

    public MaxParallelismPlannerOnlyOnDemand() {
	super(NAME, new BasicMapper(), new MaxParallelismScheduler(true), new BasicInputDataValidator());

    }

}
