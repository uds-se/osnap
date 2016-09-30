package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map.Entry;

import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

public class SummarySchedule implements IResultProcessor {

    private final String DATA_ROW_FORMAT = "%s,%s,%s,%s,%s,%s"; // CSV Output
    private final String FILE_NAME = "%s.summary.csv";

    @Override
    public void process(Result result) {
	for (Entry<String, Schedule> entry : result.getSchedules().entrySet()) {
	    File outputFile = new File(String.format(FILE_NAME, entry.getKey()));
	    outputFile.delete();

	    try (FileOutputStream fos = new FileOutputStream(outputFile); PrintStream ps = new PrintStream(fos);) {
		Schedule schedule = entry.getValue();

		// Count the number of total number of snapshots
		int sCount = 0;
		int sTestJob = 0;
		for (List<Job> jobs : schedule.jobsDistribution.values()) {
		    for (Job job : jobs) {
			if (job.snapshot)
			    sCount++;
			else
			    sTestJob++;
		    }
		}
		// Print the csv line entry
		ps.println(String.format(DATA_ROW_FORMAT, //
			schedule.objective, //
			schedule.getFinalCost(), //
			schedule.getFinalTime(), //
			schedule.totalComputationTime, //
			sCount, //
			sTestJob));

	    } catch (Exception e) {
		// Skip this continue with next one
		e.printStackTrace();
	    }
	}
    }

}
