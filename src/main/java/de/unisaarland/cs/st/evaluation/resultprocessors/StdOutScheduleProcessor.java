package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.PrintStream;
import java.util.Map.Entry;

import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

public class StdOutScheduleProcessor implements IResultProcessor {

    @Override
    public void process(Result result) {
	// TODO Make this configurable ?
	PrintStream s = new PrintStream(System.out);

	for (Entry<String, Schedule> entry : result.getSchedules().entrySet()) {
	    Schedule schedule = entry.getValue();
	    s.println("Schedule for: " + entry.getKey());
	    s.println(schedule);
	}
    }

}
