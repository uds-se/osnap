package de.unisaarland.cs.st.cplex;

import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactoryCPLEX;

/**
 * 
 * @author gambi
 *
 */
public class MySolverFactoryCPLEX extends SolverFactoryCPLEX {

    @Override
    protected Solver getInternal() {
	return new MySolverCPLEX();
    }

}
