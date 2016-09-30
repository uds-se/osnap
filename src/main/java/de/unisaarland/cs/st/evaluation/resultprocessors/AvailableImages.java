package de.unisaarland.cs.st.evaluation.resultprocessors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.esotericsoftware.yamlbeans.YamlWriter;

import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.evaluation.IResultProcessor;
import de.unisaarland.cs.st.evaluation.Result;

public class AvailableImages implements IResultProcessor {

    private final String FILE_NAME = "%s.available-images.yml";

    @Override
    public void process(Result result) {
	for (Entry<String, Schedule> entry : result.getSchedules().entrySet()) {
	    try {
		String plannerName = entry.getKey();
		String outputFileName = String.format(FILE_NAME, plannerName);
		Schedule schedule = entry.getValue();
		//
		Set<Image> resultingImages = new HashSet<Image>(result.getAvailableImages());
		// The any snapshot that was created by the planner
		// Count the number of total number of snapshots
		for (List<Job> jobs : schedule.jobsDistribution.values()) {
		    for (Job job : jobs) {
			if (job.snapshot)
			    resultingImages.add(job.image);
		    }

		}

		// Write to file !
		YamlWriter yamlWriter = new YamlWriter(new FileWriter(new File(outputFileName)));
		yamlWriter.write(resultingImages);
		yamlWriter.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }
}
