package de.unisaarland.cs.st.evaluation.resultprocessors;

public class Experiment1ChainedResultProcessor extends ChainedResultProcessor {

    public Experiment1ChainedResultProcessor() {
	super();
	//
	addResultProcessor(new StdOutSummaryScheduleProcessor());
	addResultProcessor(new StdOutPackageTiming());
    }
}
