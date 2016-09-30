package de.unisaarland.cs.st.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.unisaarland.cs.st.cplex.MySolverCPLEX;
import de.unisaarland.cs.st.cplex.MySolverCPLEX.Hook;
import de.unisaarland.cs.st.cplex.MySolverFactoryCPLEX;
import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.data.TestJob;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import net.sf.javailp.Constraint;
import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.VarType;

/**
 * Define the final Schedule. The schedule contains both snapshots jobs and test
 * jobs, and their allocation to reserved and on-demand processing units.
 * 
 * 
 * Input of the problem - Jobs (test or snapshot), execution time p_i - Job
 * Precedence (y_i_j) - Reserved Instances R
 * 
 * 
 * TODO Make this parametric
 * 
 * Input: -
 * 
 * Output: - Schedule of Snapshots and TestBatches
 * 
 * @author alessiogambi
 *
 */
public class ILPScheduler implements ITestJobScheduler {

    protected final static Logger logger = Logger.getLogger(ILPScheduler.class);
    public final static int BIG_M = 300000; // Big M method - Large constant

    protected Problem problem;
    protected Result result;

    public final static String t_i = "start_job_%s";
    public final static String T_i = "end_job_%s";
    public final static String od_i_m = "job_%s_in_on_demand_instance_%s";
    public final static String r_i_m = "job_%s_in_reserved_instance_%s";
    public final static String y_i_j = "job_%s_before_job_%s";

    /**
     * Inputs of the problem - p_i duration of Job_i (either this is a test or a
     * snapshot) - J number of jobs (this is also the maximum amount of
     * instances that can be used single 1 job must be deployed to one instance,
     * but reserved instances can be used to run more than 1 job) - pr_i_j job
     * precedences (this can be snapshot creation and usage, but also continuous
     * testing) - R available Reserved instances. 0 to jobs.size()
     * 
     * 
     * Variables of the Problem - r_i_j deployment of Job_i on Reserved Instance
     * j - od_i_j deployment of Job_i on On-Demand Instance j - y_i_j precedence
     * of jobs Job_i must end before Job_j can start - t_i start time of Job_i -
     * T_i end time of Job_i - Goal.EXECUTION_TIME - Goal.EXECUTION_COST
     * 
     * Goal - Min Goal.EXECUTION_TIME - Min COST - Min Utility( a*TIME + b*COST)
     * 
     * Solution - r_i_j deployment of Job_i on Reserved Instance j - od_i_j
     * deployment of Job_i on On-Demand Instance j - y_i_j precedence of jobs
     * Job_i must end before Job_j can start
     * 
     * Optional Constraints - MAX Cost - MAX Time - MAX OD (maximum number of On
     * Demand instances)
     * 
     * Constraints - Timing 1. For All i in Jobs/J, Goal.EXECUTION_TIME >= T_i
     * (The total time is biggest than all the others) 2a, 2b. For All i in
     * Jobs/J, t_i >= 0, T_i >= t_i + p_i (follows T_i >= t_i) 3.
     * 
     * - Resources 1. SUM over i in Jobs SUM over j in Resources (r_i_j +
     * od_i_j) <= J (Cannot use more resources than needed) - Not 100% accurate
     * because it enforces to place a snapshot job in a single execution while
     * this might not be necessary (start, snapshot, continue)
     * 
     * - Deployment 1. For All i in Jobs, SUM (j in Resources/J) r_i_j + od_i_j
     * = 1 (All jobs must be allocated and each job must be allocated once and
     * only once not matter where Reserved/OnDemans) 2. For All j in Resources,
     * SUM (i in Jobs/J) od_i_j = 1 (OnDemand instances must host one and only
     * one job)
     * 
     * - Ordering 1. For All i, i' in Jobs/J y_i_j = pr_i_j (Order must satisfy
     * pr_i_j (snapshot def/use and CI jobs)) - Not that using = might be too
     * strong, probably we should aim for >= and <= instead
     * 
     * - Combined Constraints. Those constraints links various variables
     * together
     * 
     * 1. For all i,j in Jobs | i > j, For m in R, y_i_j + y_j_i >= r_i_m +
     * r_j_m -1 (Partial Ordering implied by the use of Reserved instances) 2.
     * For all i,j in Jobs | i != j, tj >= t_i + SUM (on m in R) r_i_m * p_i_m -
     * BIG_K(1 - y_i_j) (The BIG_K thing is to encode the IF-THEN-ELSE,
     * basically this says that a job cannot start before all the jobs before it
     * finish... if any)
     * 
     * 
     * @param testBatches
     * @param R
     * @param precedence
     * @param fixedSnapshotCreationTime
     * @param testBatchToSnapshot
     * @param M
     */

    protected CloudModel cloudModel;
    protected Set<Job> inputJobs;
    protected Goal goal;

    private final static Comparator<Job> startTimeComparator = new Comparator<Job>() {

	@Override
	public int compare(Job o1, Job o2) {
	    return (int) (o1.startTime - o2.startTime);
	}
    };

    // §

    // Number of test jobs !
    int R;
    int maxOnDemandInstances;
    // long[] p;
    // int[][] pr;
    // float alpha, beta;

    protected void setMaxOnDemandInstances() {
	if (goal.maxOnDemandInstances >= 0) {
	    logger.info("Setting maxOnDemandInstances to " + goal.maxOnDemandInstances);
	    this.maxOnDemandInstances = goal.maxOnDemandInstances;// Count from
								  // 0
	} else {
	    // Set this to the maximum !
	    logger.info("Setting maxOnDemandInstances to MAX");
	    this.maxOnDemandInstances = inputJobs.size();
	}
    }

    protected void createTheProblem() {
	this.problem = new Problem();
	// Is this a problem if goal.nMax is smaller ?
	this.R = cloudModel.getAvailableReservedInstances(); // Let's count
							     // from 0 !

	//
	this.setMaxOnDemandInstances();

	/*
	 * Introduce the Variables
	 */
	problem.setVarType(String.format(Goal.EXECUTION_TIME), VarType.INT);
	problem.setVarLowerBound(String.format(Goal.EXECUTION_TIME), 0);

	problem.setVarType(String.format(Goal.EXECUTION_COST), VarType.INT);
	problem.setVarLowerBound(String.format(Goal.EXECUTION_COST), 0);

	//

	/*
	 * Job related variables
	 */
	// for (int i = 0; i < J; i++) {
	for (Job i : inputJobs) {
	    problem.setVarType(String.format(t_i, i.id), VarType.INT);
	    problem.setVarLowerBound(String.format(t_i, i.id), 0);

	    problem.setVarType(String.format(T_i, i.id), VarType.INT);
	    problem.setVarLowerBound(String.format(T_i, i.id), 0);

	    for (Job j : inputJobs) {
		if (i.equals(j))
		    continue;

		problem.setVarType(String.format(y_i_j, i.id, j.id), VarType.BOOL);

	    }
	}

	/*
	 * Deployment related variables. This is either 0 or 1 but it says that
	 * is continuous
	 */
	for (Job i : inputJobs) {
	    for (int m = 0; m < R; m++) {
		problem.setVarType(String.format(r_i_m, i.id, m), VarType.BOOL);
		// Boundaries automatically set
	    }
	}

	for (Job i : inputJobs) {
	    for (int m = 0; m < maxOnDemandInstances; m++) {
		problem.setVarType(String.format(od_i_m, i.id, m), VarType.BOOL);
		// Boundaries automatically set
	    }
	}

	/*
	 * Define the Goal. Limitation of ILP -> single objective, but not a
	 * real problem for us
	 */
	Linear aggregateObjectFunction = new Linear();
	// keep always time there... never really 0.0 !
	aggregateObjectFunction.add((goal.getAlpha() > 0.0) ? goal.getAlpha() : 0.01,
		String.format(Goal.EXECUTION_TIME));
	//
	aggregateObjectFunction.add(goal.getBeta(), String.format(Goal.EXECUTION_COST));
	problem.setObjective(aggregateObjectFunction, OptType.MIN);

	/*
	 * Constraints
	 * 
	 */

	/*
	 * - Timing 1. For All i in Jobs/J, Goal.EXECUTION_TIME >= T_i (The
	 * total time is biggest than all the others) 2a, 2b. For All i in
	 * Jobs/J, t_i >= 0, T_i >= t_i + p_i (follows T_i >= t_i)
	 */

	// 1
	for (Job i : inputJobs) {
	    Linear jobMakespan = new Linear();
	    jobMakespan.add(1, String.format(Goal.EXECUTION_TIME));
	    jobMakespan.add(-1, String.format(T_i, i.id));
	    problem.add(jobMakespan, ">=", 0);
	}

	// Total cost is the sum
	Linear totalCost = new Linear();
	totalCost.add(1, Goal.EXECUTION_COST);

	for (Job i : inputJobs) {
	    // Cost if deployed on reserved
	    for (int m = 0; m < R; m++) {
		totalCost.add(
			-1 * this.cloudModel.getCostOfReservedInstancePerBUT()
				* ((int) Math.ceil((double) i.processingTime / (double) this.cloudModel.getBUT())),
			String.format(r_i_m, i.id, m));
	    }
	    // Cost if deployed on on-demand
	    for (int m = 0; m < maxOnDemandInstances; m++) {
		totalCost.add(
			-1 * this.cloudModel.getCostOfOnDemandInstancePerBUT()
				* ((int) Math.ceil((double) i.processingTime / (double) this.cloudModel.getBUT())),
			String.format(od_i_m, i.id, m));
	    }
	}
	//
	problem.add(new Constraint("total cost", totalCost, "=", 0));

	for (Job i : inputJobs) {
	    // 2a
	    Linear startTime = new Linear();
	    startTime.add(1, String.format(t_i, i.id));
	    problem.add(new Constraint(startTime, ">=", 0));

	    Linear endTime = new Linear();
	    endTime.add(1, String.format(T_i, i.id));
	    endTime.add(-1, String.format(t_i, i.id));
	    problem.add(endTime, "=", i.processingTime);
	}

	/*
	 * - Resources 1. SUM over i in Jobs SUM over j in Resources (r_i_j +
	 * od_i_j) <= J (Cannot use more resources than needed) - Not 100%
	 * accurate because it enforces to place a snapshot job in a single
	 * execution while this might not be necessary (start, snapshot,
	 * continue)
	 */

	// If we cannot use onDemand instances this constrain should not be
	// there !
	Linear onDemandResources = new Linear();
	for (int m = 0; m < maxOnDemandInstances; m++) {
	    // Sum
	    for (Job i : inputJobs) {
		onDemandResources.add(1, String.format(od_i_m, i.id, m));
	    }
	}
	// Add the constraint only if necessary
	if (maxOnDemandInstances > 0) {
	    problem.add(new Constraint(onDemandResources, "<=", maxOnDemandInstances));
	}

	/*
	 * - Deployment 1. For All i in Jobs, SUM (j in Resources/J) r_i_j +
	 * od_i_j = 1 (All jobs must be allocated and each job must be allocated
	 * once and only once not matter where Reserved/OnDemans) 2. For All j
	 * in Resources, SUM (i in Jobs/J) od_i_j = 1 (OnDemand instances must
	 * host one and only one job)
	 */

	// 1
	for (Job i : inputJobs) {
	    Linear allocation = new Linear();
	    for (int m = 0; m < R; m++) {
		allocation.add(1, String.format(r_i_m, i.id, m));
	    }
	    for (int m = 0; m < maxOnDemandInstances; m++) {
		allocation.add(1, String.format(od_i_m, i.id, m));
	    }
	    problem.add(new Constraint(allocation, "=", 1));
	}

	// 2
	for (int m = 0; m < maxOnDemandInstances; m++) {
	    Linear singleOnDemand = new Linear();
	    for (Job i : inputJobs) {
		singleOnDemand.add(1, String.format(od_i_m, i.id, m));
	    }
	    problem.add(new Constraint(singleOnDemand, "<=", 1));
	}

	/*
	 * - Ordering 1. For All i, i' in Jobs/J y_i_j = pr_i_j (Order must
	 * satisfy pr_i_j (snapshot def/use and CI jobs)) - Not that using =
	 * might be too strong, probably we should aim for >= and <= instead
	 */
	//

	// Note J and I are opposite. if j depends on i, i must come before,
	// then y_i,j = 1
	for (Job j : inputJobs) {
	    for (Job i : j.dependsOn) {
		Linear snapshotBeforeTest = new Linear();
		snapshotBeforeTest.add(1, String.format(y_i_j, i.id, j.id));
		problem.add(snapshotBeforeTest, "=", 1);

		Linear testAfterSnapshot = new Linear();
		testAfterSnapshot.add(1, String.format(y_i_j, j.id, i.id));
		problem.add(testAfterSnapshot, "=", 0);
	    }
	}

	/*
	 * - Combined Constraints. Those constraints links various variables
	 * together
	 * 
	 * 1. For all i,j in Jobs | i > j, For m in R, y_i_j + y_j_i >= r_i_m +
	 * r_j_m -1 (Partial Ordering implied by the use of Reserved instances)
	 * 
	 * 2. For all i,j in Jobs | i != j, tj >= t_i + SUM (on m in R) r_i_m *
	 * p_i_m - BIG_K(1 - y_i_j)
	 * 
	 * 3. Global Constraint ? For all i,j in Jobs | i != j if y_i_j >= 1
	 * then t_j >= T_i becomes y_i_j < 1 OR t_j >= t_i + p_i
	 * 
	 * t_j -t_i -p_i - BIG_K * y_i_j >= - BIG_K
	 * 
	 * 
	 * 
	 * when y_i_j = 0 y_i_j = 0
	 * 
	 * 
	 * 
	 * 3b. if y_j_i >= 1 then t_i >= T_j becomes y_j_i < 1 OR T_j <= t_i
	 * 
	 * 
	 * 
	 * (The BIG_K thing is to encode the IF-THEN-ELSE, basically this says
	 * that a job cannot start before all the jobs before it finish... if
	 * any)
	 */

	// 1 - Force on Reserved:
	for (Job i : inputJobs) {
	    for (Job j : inputJobs) {
		if (i.id > j.id) {
		    for (int m = 0; m < R; m++) {

			Linear orderingOnReserved = new Linear();
			orderingOnReserved.add(1, String.format(y_i_j, i.id, j.id));
			orderingOnReserved.add(1, String.format(y_i_j, j.id, i.id));
			orderingOnReserved.add(-1, String.format(r_i_m, i.id, m));
			orderingOnReserved.add(-1, String.format(r_i_m, j.id, m));

			problem.add(new Constraint(orderingOnReserved, ">=", -1));
		    }
		}
	    }
	}

	// Global Ordering. Order relation must be valid everywhere
	// Why m starts from 1 ? m=1 produces the expected results... but why so
	// ?!

	// Force on On-Demand -> We need to test this !
	for (Job i : inputJobs) {
	    for (Job j : inputJobs) {
		if (i.id != j.id) {
		    if (i != j) {

			Linear timeSequence = new Linear();
			timeSequence.add(1, String.format(t_i, j.id));
			timeSequence.add(-1, String.format(t_i, i.id));

			// Same as before but not limited to a specific resource

			for (int m = 0; m < R; m++) {
			    timeSequence.add(-i.processingTime, String.format(r_i_m, i.id, m));
			}

			for (int m = 0; m < maxOnDemandInstances; m++) {
			    timeSequence.add(-i.processingTime, String.format(od_i_m, i.id, m));
			}

			timeSequence.add(-BIG_M, String.format(y_i_j, i.id, j.id));

			problem.add(timeSequence, ">=", -BIG_M);
			// + i.processingTime Why this ?!
		    }
		}
	    }
	}

	// Optional Constraints = NOTE that those can make the solution
	// unfeasible !
	if (goal.maxCost > 0) {
	    Linear maxCost = new Linear();
	    maxCost.add(1, Goal.EXECUTION_COST);
	    problem.add(maxCost, "<=", goal.maxCost);
	}

	if (goal.maxTime > 0) {
	    Linear maxTime = new Linear();
	    maxTime.add(1, Goal.EXECUTION_TIME);
	    problem.add(maxTime, "<=", goal.maxTime);
	}

	logger.trace(problem);
    }

    private Job findJobById(int id) {
	for (Job job : inputJobs) {
	    if (job.id == id) {
		return job;
	    }
	}
	return null;
    }

    @Override
    public Schedule solve(long startTime, Set<Job> _jobs, CloudModel cloudModel, Goal goal) {
	this.inputJobs = _jobs;
	this.cloudModel = cloudModel;
	this.goal = goal;
	//
	SolverFactory factory = null;
	Solver solver = null;
	//
	long endTime = -1;

	createTheProblem();

	logger.trace(problem);

	try {
	    factory = new MySolverFactoryCPLEX();
	    if (logger.getLevel() != null && logger.getLevel().isGreaterOrEqual(Level.DEBUG)) {
		factory.setParameter(Solver.VERBOSE, 100);
	    } else {
		factory.setParameter(Solver.VERBOSE, 0);
	    }

	    factory.setParameter(Solver.TIMEOUT, 600); // 10 mins ?

	    solver = factory.get();

	    // Shall we activate the heuristic also for the
	    // DeploymentAwareScheduler?
	    // Introduce mods
	    ((MySolverCPLEX) solver).addHook(new Hook() {

		@Override
		public void call(IloCplex cplex, Map<Object, IloNumVar> varToNum) {
		    try {
			double relativeImprovement = 0.0005;
			long improvementObservationIntervalInSeconds = 20;
			// System.out.println("GAP Relative Improvement " +
			// relativeImprovement);
			// System.out.println("GAP Relatieve Improvement
			// observation period (sec)"
			// + improvementObservationIntervalInSeconds);
			cplex.use(new TimeLimitCallback(cplex, false, cplex.getCplexTime(),
				improvementObservationIntervalInSeconds, relativeImprovement));

			// Particular cases
		    } catch (IloException e) {
			e.printStackTrace();
		    }

		}
	    });

	    //

	    result = solver.solve(problem);
	    endTime = System.currentTimeMillis();

	} catch (

	java.lang.UnsatisfiedLinkError exception) {
	    exception.printStackTrace();
	    throw exception;
	}
	if (result != null) {
	    // TODO Build a schedule - we use the input jobs !
	    logger.debug(String.format("Objective Min %s * TIME + %s * COST = %s", goal.getAlpha(), goal.getBeta(),
		    result.getObjective()));
	    Map<Instance, List<Job>> jobsDistribution = new HashMap<Instance, List<Job>>();
	    //
	    for (int m = 0; m < R; m++) {
		Instance reserved = new Instance(m, true);
		List<Job> jobs = new ArrayList<Job>();
		for (Job i : inputJobs) {
		    if (result.get(String.format(r_i_m, i.id, m)).intValue() > 0) {
			i.startTime = result.get(String.format(t_i, i.id)).longValue();
			i.endTime = result.get(String.format(T_i, i.id)).longValue();
			jobs.add(i);
		    }
		}
		//
		Collections.sort(jobs, startTimeComparator);
		jobsDistribution.put(reserved, jobs);
	    }

	    for (int m = 0; m < maxOnDemandInstances; m++) {

		List<Job> jobs = new ArrayList<Job>();

		for (Job i : inputJobs) {
		    if (result.get(String.format(od_i_m, i.id, m)).intValue() > 0) {
			i.startTime = result.get(String.format(t_i, i.id)).longValue();
			i.endTime = result.get(String.format(T_i, i.id)).longValue();
			jobs.add(i);
		    }
		}

		// Add only if not empty !
		if (jobs.size() > 0) {
		    Instance ondemand = new Instance(m, false);
		    // Sort per start-time
		    Collections.sort(jobs, startTimeComparator);
		    jobsDistribution.put(ondemand, jobs);
		}
	    }

	    // TODO Probably we can check if the values got by ILP are the same
	    // that we compute from jobsDistributiong
	    long computationTime = endTime - startTime;
	    return new Schedule(goal, cloudModel, jobsDistribution, computationTime);

	    // return new Schedule(result.getObjective().doubleValue(),
	    // result.get(String.format(Goal.EXECUTION_COST)).intValue(),
	    // result.get(String.format(Goal.EXECUTION_TIME)).intValue(),
	    // jobsDistribution, (endTime - startTime)// Computation
	    // // time
	    //
	    // );
	} else {
	    throw new RuntimeException("CANNOT FIND A SOLUTION !");
	}
    }

    @Override
    public Schedule solve(Set<Job> jobs, CloudModel cloudModel, Goal goal) {
	// This is only to compute the time to solve the scheduling problem
	return solve(System.currentTimeMillis(), jobs, cloudModel, goal);
    }

    /*
     * Taken from examples.MIPex4: Avoid to spend long time in trying to solve
     * optimally this problem. If not enough improvement was achieved, we just
     * stop researching and return the current optimal solution
     */
    static class TimeLimitCallback extends IloCplex.MIPInfoCallback {
	IloCplex _cplex;
	boolean _aborted;
	double _observationPeriod;
	double _timeStart;
	double _relativeImprovement;

	TimeLimitCallback(IloCplex cplex, boolean aborted, double timeStart, double observationPeriod,
		double relativeImprovement) {
	    _cplex = cplex;
	    _aborted = aborted;
	    _timeStart = timeStart;
	    _observationPeriod = observationPeriod;
	    _relativeImprovement = relativeImprovement;
	}

	private double previousRelativeGap = 1.0;
	private boolean warning = false;

	public void main() throws IloException {

	    if (!_aborted && hasIncumbent()) {

		// Read the value
		double gap = getMIPRelativeGap();
		double time = _cplex.getCplexTime();

		// Toggle status
		if (previousRelativeGap - gap < _relativeImprovement && !warning) {
		    // Switch to warning
		    // System.out.println("Warning: Not enough improvement");
		    warning = !warning;
		    _timeStart = time;
		} else if (previousRelativeGap - gap > _relativeImprovement && warning) {
		    // System.out.println("Back to normal");
		    warning = !warning;
		}

		// Check abort condition
		if (time - _timeStart > _observationPeriod) {
		    logger.warn("Not enough improvement in the past " + _observationPeriod + " secs. Relative gap = "
			    + gap + "%, quitting.");
		    _aborted = true;
		    abort();
		}
		// Update the reading
		previousRelativeGap = gap;
	    }
	}
    }

    // public static void main(String[] args) {
    // // Probably a Factory for this:
    // CloudModel testCloud = new CloudModel(60, 0, 1, 1);
    // CloudModel testCloud2 = new CloudModel(60, 0, 1, 0);
    //
    // // once for one problem
    // Goal goal = new Goal(1.0, 0.0);
    // goal.maxOnDemandInstances = 0;
    //
    // Goal goal2 = new Goal(1.0, 0.0);
    // goal2.maxOnDemandInstances = 5;
    //
    // // Create a simple problem
    // Set<Job> inputJobs_1 = new HashSet<Job>();
    // Set<Job> inputJobs_2 = new HashSet<Job>();
    //
    // Job j1_1 = Job.createNewJob(1);
    // j1_1.testJob = new TestJob();
    // Job j2_1 = Job.createNewJob(30);
    // j2_1.testJob = new TestJob();
    // Job j3_1 = Job.createNewJob(50);
    // j3_1.testJob = new TestJob();
    //
    // Job j1_2 = Job.createNewJob(1);
    // j1_2.testJob = j1_1.testJob;
    // Job j2_2 = Job.createNewJob(30);
    // j2_2.testJob = j2_1.testJob;
    // Job j3_2 = Job.createNewJob(50);
    // j3_2.testJob = j3_1.testJob;
    //
    // // Precedence
    // j3_1.dependsOn.add(j2_1);
    // j3_1.dependsOn.add(j1_1);
    // j2_1.dependsOn.add(j1_1);
    //
    // j3_2.dependsOn.add(j2_2);
    // j3_2.dependsOn.add(j1_2);
    // j2_2.dependsOn.add(j1_2);
    //
    // inputJobs_1.add(j1_1);
    // inputJobs_1.add(j2_1);
    // inputJobs_1.add(j3_1);
    //
    // inputJobs_2.add(j1_2);
    // inputJobs_2.add(j2_2);
    // inputJobs_2.add(j3_2);
    //
    // // TA, SNAPSHOT A,B, TB
    // // processingTimes = new long[] { 20 };
    // // defUses = new int[][] { { 0 } };
    //
    // // Dependency informations are within the Job object
    //
    // ILPScheduler ts = new ILPScheduler();
    // Schedule schedule_1 = ts.solve(inputJobs_1, testCloud, goal);
    // Schedule schedule_2 = new ILPScheduler().solve(inputJobs_2, testCloud2,
    // goal2);
    // Schedule schedule_3 = ts.solve(inputJobs_2, testCloud2, goal2);
    // System.out.println(schedule_1);
    // System.out.println(schedule_2);
    // // This must be the same as schedule 2 !
    // System.out.println(schedule_3);
    // }

}
