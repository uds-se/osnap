package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.util.ArrayList;
import java.util.List;

import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

// This accumulate results in the same execution context. Note that this recreates it's output everytime.
// The design is bad but cannot do otherwise
public class ResultMerger implements IResultProcessor {

    private final static List<Result> accumulator = new ArrayList<Result>();
    private final static String FILE_FORMAT = "results.%03d";

    void processAll() {
	//
	//
	Result mergedResult = new Result(); // This can be also reused

	for (Result result : accumulator) {
	    mergedResult.mergeWith(result);
	    // System.err.println(mergedResult);
	}

	mergedResult.toFile(String.format(FILE_FORMAT, accumulator.size()));
    }

    @Override
    public void process(Result result) {
	// System.out.println("ResultMerger.process() " + result);
	accumulator.add(result);
	processAll();
    }

}
