package de.unisaarland.cs.st.evaluation.resultprocessors;

import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

public class StdOutResultProcessor implements IResultProcessor {

    @Override
    public void process(Result result) {
	System.out.println(result.prettyPrint());
    }

}
