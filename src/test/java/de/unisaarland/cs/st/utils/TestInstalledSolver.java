package de.unisaarland.cs.st.utils;

import net.sf.javailp.Linear;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.SolverFactoryCPLEX;
import net.sf.javailp.SolverFactoryLpSolve;

public class TestInstalledSolver {
	public static void main(String[] args) {
		SolverFactory factory = null;
		Solver solver = null;
		Result result = null;
		Problem problem = new Problem();
		problem.setObjective(new Linear());

		System.out.println("TestInstalledSolver.main() properties: ");
		System.out.println(System.getProperties());

		System.out.println("TestInstalledSolver.main() path.java: ");
		System.out.println(System.getProperty("java.library.path"));

		try {
			System.out.println("Cplex");
			factory = new SolverFactoryCPLEX();
			factory.setParameter(Solver.VERBOSE, 0);
			factory.setParameter(Solver.TIMEOUT, 100);
			solver = factory.get();
			result = solver.solve(problem);
			System.out.println(" available");
		} catch (UnsatisfiedLinkError e) {
			System.out.println(" not available");
		}

		try {
			System.out.println("LpSolve");
			factory = new SolverFactoryLpSolve();

			factory.setParameter(Solver.VERBOSE, 0);
			factory.setParameter(Solver.TIMEOUT, 100);
			solver = factory.get();
			result = solver.solve(problem);
			System.out.println(" available");
		} catch (UnsatisfiedLinkError e) {
			System.out.println(" not available");
		}

	}
}
