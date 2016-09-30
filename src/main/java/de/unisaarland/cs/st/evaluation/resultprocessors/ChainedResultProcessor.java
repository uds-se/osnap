package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.util.ArrayList;
import java.util.List;

import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

public class ChainedResultProcessor implements IResultProcessor {

    private List<IResultProcessor> resultProcessors = new ArrayList<IResultProcessor>();

    public void addResultProcessor(IResultProcessor resultProcessor) {
	this.resultProcessors.add(resultProcessor);
    }

    /**
     * Transpose the output: invoke all the processor for each test execution
     * planner
     */
    @Override
    public void process(Result result) {

	for (Result r : result.filterByPlanner())
	    for (IResultProcessor resultProcessor : resultProcessors) {
		try {
		    resultProcessor.process(r);
		} catch (Throwable t) {
		    System.err.println("Failed Result Processor " + resultProcessor + " with: ");
		    t.printStackTrace();
		}
	    }
    }

}
