package de.unisaarland.cs.st.planners;

import de.unisaarland.cs.st.mappers.BasicMapper;
import de.unisaarland.cs.st.schedulers.MinLoadScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;

/**
 * 
 * This Test Execution Planner implements the maximum parallelism planner that
 * is 1 test job for 1 instance (regarless instances are reseved or not!)
 * 
 * @author gambi
 *
 */
public class MinLoadPlanner extends TestExecutionPlanner {

    public static final String NAME = "MinLoadPlanner";

    public MinLoadPlanner() {
	super(NAME, new BasicMapper(), new MinLoadScheduler(), new BasicInputDataValidator());

    }

}
