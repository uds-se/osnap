package de.unisaarland.cs.st.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Job;
import de.unisaarland.cs.st.data.Schedule;

public class SchedulePlotter {

    public static final String PLOT_FOLDER = "plot";
    public static final String TMP_DIR_PREFIX = "osnap-test-plotter";
    public static final String GNUPLOT = "/usr/local/bin/gnuplot";
    public static final String PYTHON = "/usr/local/bin/python";

    /**
     * Creates a tmp folder and invokes the gannt.py / gnuplot script
     * 
     * @param schedule
     * @throws IOException
     * @throws InterruptedException
     */

    public static File plotSchedule(Schedule schedule) throws IOException, InterruptedException {
	return plotSchedule(schedule, true);
    }

    public static void plotScheduleToFile(Schedule schedule, File outputFile) throws IOException, InterruptedException {
	File epsFile = plotSchedule(schedule, true);
	epsFile.renameTo(outputFile);

    }

    private static File plotSchedule(Schedule schedule, boolean deleteTmpFolder)
	    throws IOException, InterruptedException {

	// Make TmpFolder
	Path tmpDir = Files.createTempDirectory(TMP_DIR_PREFIX);

	if (deleteTmpFolder) {
	    tmpDir.toFile().deleteOnExit();
	}

	// Generate the DataFile Corresponding to schedule
	File dataFile = new File(tmpDir.toFile(), "schedule.txt");
	File scheduleGplFile = new File(tmpDir.toFile(), "schedule.gpl");
	File ganntPyFile = new File(tmpDir.toFile(), "gannt.py");
	File colorCfgFile = new File(tmpDir.toFile(), "colors.cfg");
	File mainGplFile = new File(tmpDir.toFile(), "main.gpl");
	File scheduleEpsFile = new File(tmpDir.toFile(), "schedule.eps");

	PrintWriter writer = new PrintWriter(dataFile, "UTF-8");
	for (Entry<Instance, List<Job>> instance : schedule.jobsDistribution.entrySet()) {

	    if (instance.getValue().isEmpty())
		continue;

	    String resource = null;
	    if (instance.getKey().isReserved()) {
		resource = "R" + instance.getKey().getId();
	    } else {
		resource = "OD" + instance.getKey().getId();
	    }

	    for (Job job : instance.getValue()) {
		String task = null;
		if (job.snapshot) {
		    task = "Snapshot";
		} else {
		    task = "Test";
		}

		writer.println(String.format("%s\t%d\t%d\t%s", resource, job.startTime, job.endTime, task));
	    }
	}
	writer.close();

	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	InputStream inputStream = classLoader.getResourceAsStream(PLOT_FOLDER + "/gantt.py");
	byte[] buffer = new byte[inputStream.available()];
	inputStream.read(buffer);
	OutputStream outStream = new FileOutputStream(ganntPyFile);
	outStream.write(buffer);
	//
	inputStream.close();
	outStream.close();
	// TODO This might be created on the fly to match the actual content !
	inputStream = classLoader.getResourceAsStream(PLOT_FOLDER + "/colors.cfg");
	buffer = new byte[inputStream.available()];
	inputStream.read(buffer);
	outStream = new FileOutputStream(colorCfgFile);
	outStream.write(buffer);
	//
	inputStream.close();
	outStream.close();
	//
	writer = new PrintWriter(mainGplFile);// , "UTF-8");
	// writer.println("reset");
	writer.println("set terminal postscript eps color solid");
	writer.println("set size ratio 0.4");
	writer.println("set output \"" + scheduleEpsFile.getAbsolutePath() + "\"");
	writer.println("load \"" + scheduleGplFile.getAbsolutePath() + "\"");
	writer.println("unset output;");
	writer.println("");
	writer.close();
	//
	String[] cmd = new String[] { PYTHON, ganntPyFile.getAbsolutePath(), //
		"-c", colorCfgFile.getAbsolutePath(), //
		"-t", "Schedule", // Better TITLE
		"-o", scheduleGplFile.getAbsolutePath(), //
		dataFile.getAbsolutePath() };
	Process p = Runtime.getRuntime().exec(cmd);
	if (p.waitFor() != 0) {
	    throw new RuntimeException("Failed " + Arrays.toString(cmd));
	}
	//
	List<String> params = java.util.Arrays.asList(GNUPLOT, mainGplFile.getAbsolutePath());
	ProcessBuilder b = new ProcessBuilder(params);
	p = b.start();
	// Thread.currentThread().sleep(3000);
	int exitCode = p.waitFor();
	if (exitCode != 0) {
	    throw new RuntimeException("Failed " + Arrays.toString(cmd));
	}

	return scheduleEpsFile;
    }

}
