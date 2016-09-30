package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.PrintStream;

import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

public class StdOutListPlanners implements IResultProcessor {

    @Override
    public void process(Result result) {
	PrintStream ps = new PrintStream(System.out);
	for (String name : result.getPlannerNames())
	    ps.println(name);
    }

}
