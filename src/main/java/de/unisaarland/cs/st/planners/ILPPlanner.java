package de.unisaarland.cs.st.planners;

import de.unisaarland.cs.st.mappers.BasicMapper;
import de.unisaarland.cs.st.schedulers.ILPScheduler;
import de.unisaarland.cs.st.util.BasicInputDataValidator;

/**
 * 
 * This Test Execution Planner uses Integer Linear Programming (ILP) to plan the
 * test scheduling. Note that this is cost-and-time aware
 * 
 * @author gambi
 *
 */
public class ILPPlanner extends TestExecutionPlanner {

    public static final String NAME = "ILPPlanner";

    public ILPPlanner() {
	super(NAME, new BasicMapper(), new ILPScheduler(), new BasicInputDataValidator());

    }

}
