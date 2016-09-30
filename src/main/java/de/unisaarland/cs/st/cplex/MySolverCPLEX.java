package de.unisaarland.cs.st.cplex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import net.sf.javailp.Constraint;
import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.ResultImpl;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverCPLEX;
import net.sf.javailp.Term;
import net.sf.javailp.VarType;

/**
 * 
 * @author gambi
 *
 */
public class MySolverCPLEX extends SolverCPLEX {

    private static Logger logger = Logger.getLogger(MySolverCPLEX.class);

    public interface Hook {
	public void call(IloCplex cplex, Map<Object, IloNumVar> varToNum);
    }

    protected final Set<Hook> hooks = new HashSet<Hook>();

    public void addHook(Hook hook) {
	hooks.add(hook);
    }

    public void removeHook(Hook hook) {
	hooks.remove(hook);
    }

    public Result solve(Problem problem) {
	Map<IloNumVar, Object> numToVar = new HashMap<IloNumVar, Object>();
	Map<Object, IloNumVar> varToNum = new HashMap<Object, IloNumVar>();

	try {
	    IloCplex cplex = new IloCplex();

	    cplex.setName(UUID.randomUUID().toString());

	    initWithParameters(cplex);

	    for (Object variable : problem.getVariables()) {
		VarType varType = problem.getVarType(variable);
		Number lowerBound = problem.getVarLowerBound(variable);
		Number upperBound = problem.getVarUpperBound(variable);

		double lb = (lowerBound != null ? lowerBound.doubleValue() : Double.NEGATIVE_INFINITY);
		double ub = (upperBound != null ? upperBound.doubleValue() : Double.POSITIVE_INFINITY);

		final IloNumVarType type;
		switch (varType) {
		case BOOL:
		    type = IloNumVarType.Bool;
		    // Please note this setting
		    lb = 0;
		    ub = 1;
		    break;
		case INT:
		    type = IloNumVarType.Int;
		    break;
		default: // REAL
		    type = IloNumVarType.Float;
		    break;
		}

		if (lb == Double.NEGATIVE_INFINITY) {
		    logger.warn("Variable " + variable + " has lb " + lb);
		}

		if (ub == Double.NEGATIVE_INFINITY) {
		    logger.warn("Variable " + variable + " has ub " + ub);
		}

		IloNumVar numVar = cplex.numVar(lb, ub, type);
		numToVar.put(numVar, variable);
		varToNum.put(variable, numVar);
	    }

	    for (Constraint constraint : problem.getConstraints()) {
		IloLinearNumExpr lin = cplex.linearNumExpr();
		Linear linear = constraint.getLhs();
		convert(linear, lin, varToNum);

		double rhs = constraint.getRhs().doubleValue();

		switch (constraint.getOperator()) {
		case LE:
		    cplex.addLe(lin, rhs);
		    break;
		case GE:
		    cplex.addGe(lin, rhs);
		    break;
		default: // EQ
		    cplex.addEq(lin, rhs);
		}
	    }

	    if (problem.getObjective() != null) {
		IloLinearNumExpr lin = cplex.linearNumExpr();
		Linear objective = problem.getObjective();
		convert(objective, lin, varToNum);

		if (problem.getOptType() == OptType.MIN) {
		    cplex.addMinimize(lin);
		} else {
		    cplex.addMaximize(lin);
		}
	    }

	    for (Hook hook : hooks) {
		hook.call(cplex, varToNum);
	    }

	    if (!cplex.solve()) {
		cplex.end();
		return null;
	    }

	    final Result result;
	    if (problem.getObjective() != null) {
		Linear objective = problem.getObjective();
		result = new ResultImpl(objective);
	    } else {
		result = new ResultImpl();
	    }

	    for (Entry<Object, IloNumVar> entry : varToNum.entrySet()) {
		Object variable = entry.getKey();
		IloNumVar num = entry.getValue();
		VarType varType = problem.getVarType(variable);

		double value = cplex.getValue(num);
		if (varType.isInt()) {
		    int v = (int) Math.round(value);
		    result.putPrimalValue(variable, v);
		} else {
		    result.putPrimalValue(variable, value);
		}
	    }

	    cplex.end();

	    return result;

	} catch (IloException e) {
	    e.printStackTrace();
	}

	return null;
    }

    protected void initWithParameters(IloCplex cplex) throws IloException {
	Object timeout = parameters.get(Solver.TIMEOUT);
	Object verbose = parameters.get(Solver.VERBOSE);

	if (timeout != null && timeout instanceof Number) {
	    Number number = (Number) timeout;
	    double value = number.doubleValue();
	    cplex.setParam(DoubleParam.TiLim, value);
	}
	if (verbose != null && verbose instanceof Number) {
	    Number number = (Number) verbose;
	    int value = number.intValue();

	    if (value == 0) {
		cplex.setOut(null);
	    }
	}

    }

    protected void convert(Linear linear, IloLinearNumExpr lin, Map<Object, IloNumVar> varToNum) throws IloException {
	for (Term term : linear) {
	    Number coeff = term.getCoefficient();
	    Object variable = term.getVariable();

	    IloNumVar num = varToNum.get(variable);
	    lin.addTerm(coeff.doubleValue(), num);
	}
    }

    static class MyBranch extends IloCplex.BranchCallback {
	IloNumVar[] _vars;

	MyBranch(IloNumVar[] vars) {
	    _vars = vars;
	}

	public void main() throws IloException {
	    if (!getBranchType().equals(IloCplex.BranchType.BranchOnVariable))
		return;

	    // Branch on var with largest objective coefficient
	    // among those with largest infeasibility

	    double[] x = getValues(_vars);
	    double[] obj = getObjCoefs(_vars);
	    IloCplex.IntegerFeasibilityStatus[] feas = getFeasibilities(_vars);

	    double maxinf = 0.0;
	    double maxobj = 0.0;
	    int bestj = -1;
	    int cols = _vars.length;
	    for (int j = 0; j < cols; ++j) {
		if (feas[j].equals(IloCplex.IntegerFeasibilityStatus.Infeasible)) {
		    double xj_inf = x[j] - Math.floor(x[j]);
		    if (xj_inf > 0.5)
			xj_inf = 1.0 - xj_inf;
		    if (xj_inf >= maxinf && (xj_inf > maxinf || Math.abs(obj[j]) >= maxobj)) {
			bestj = j;
			maxinf = xj_inf;
			maxobj = Math.abs(obj[j]);
		    }
		}
	    }

	    if (bestj >= 0) {
		makeBranch(_vars[bestj], x[bestj], IloCplex.BranchDirection.Up, getObjValue());
		makeBranch(_vars[bestj], x[bestj], IloCplex.BranchDirection.Down, getObjValue());
	    }
	}
    }

    static class MySelect extends IloCplex.NodeCallback {
	public void main() throws IloException {
	    long remainingNodes = getNremainingNodes64();
	    long bestnode = -1;
	    int maxdepth = -1;
	    double maxiisum = 0.0;

	    for (long i = 0; i < remainingNodes; ++i) {
		int depth = getDepth(i);
		double iisum = getInfeasibilitySum(i);
		if ((depth >= maxdepth) && (depth > maxdepth || iisum > maxiisum)) {
		    bestnode = i;
		    maxdepth = depth;
		    maxiisum = iisum;
		}
	    }
	    if (bestnode >= 0)
		selectNode(bestnode);
	}
    }
}
