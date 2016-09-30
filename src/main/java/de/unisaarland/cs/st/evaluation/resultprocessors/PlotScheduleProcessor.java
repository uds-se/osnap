package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;
import de.unisaarland.cs.st.util.SchedulePlotter;

public class PlotScheduleProcessor implements IResultProcessor {

    private final static Logger logger = Logger.getLogger(PlotScheduleProcessor.class);

    @Override
    public void process(Result result) {
	for (Entry<String, Schedule> entry : result.getSchedules().entrySet()) {
	    try {
		Schedule schedule = entry.getValue();
		File outputFile = new File(entry.getKey() + ".eps");
		SchedulePlotter.plotScheduleToFile(schedule, outputFile);
		logger.debug("Plot to " + outputFile);
	    } catch (IOException | InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }

}
