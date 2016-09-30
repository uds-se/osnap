package de.unisaarland.cs.st.schedulers;

import de.unisaarland.cs.st.data.Job;
import net.sf.javailp.Constraint;
import net.sf.javailp.Linear;

/**
 *
 * This scheduler enforces all the constraints on deployment and ordering of the
 * input jobs and produces an updated schedule
 * 
 * @author alessiogambi
 *
 */
@Deprecated
public class DeploymentAwareTestScheduler extends ILPScheduler {

    @Override
    protected void createTheProblem() {
	// Not sure why this considers only snapshot !
	super.createTheProblem();
	// Now update the Problem object with the deploymebt constraints.

	for (Job i : inputJobs) {
	    if (i.deployedOnInstance != -1) {
		for (int m = 0; m < inputJobs.size(); m++) {
		    String variable = null;
		    if (i.deployedOnReservedInstance) {
			variable = String.format(r_i_m, i.id, m);
		    } else {
			variable = String.format(od_i_m, i.id, m);
		    }

		    int value = 0;
		    if (m == i.deployedOnInstance) {
			value = 1;
		    }

		    if (problem.getVariables().contains(variable)) {
			logger.trace("DeploymentAwareTestScheduler.createTheProblem() Enforcing deployment contraint "
				+ variable + "  = " + value);
			Linear onDemandResources = new Linear();
			onDemandResources.add(1, variable);
			problem.add(new Constraint(onDemandResources, "=", value));
		    }
		}
	    }

	}
    }
}
