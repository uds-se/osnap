package de.unisaarland.cs.st.schedulers;

import java.util.Set;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;

/**
 * This interface must be implemented by components that can compute the final
 * test job scheduling
 * 
 * @author gambi
 *
 */
public interface ITestJobScheduler {

    public Schedule solve(
	    Set<Job> jobs, // Cloud jobs can be either test job or snapshot job
	    CloudModel cloudModel,
	    Goal goal
	    );
    
    public Schedule solve(
	    long startTime, // Start time needed to compute the Schedule Computation time 
	    Set<Job> jobs, // Cloud jobs can be either test job or snapshot job
	    CloudModel cloudModel,
	    Goal goal
	    );
}
