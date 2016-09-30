package de.unisaarland.cs.st.evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.evaluation.resultprocessors.StdOutResultProcessor;
import de.unisaarland.cs.st.planners.TestExecutionPlanner;

/**
 * Parse command line arguments, load data (from the DB), schedule the execution
 * of the various Planners and report the final results (stats).
 * 
 * @author gambi
 *
 */
public class EvaluationDriver {

    private final static Logger logger = Logger.getLogger(EvaluationDriver.class);

    List<TestExecutionPlanner> planners = new ArrayList<TestExecutionPlanner>();
    Set<TestJob> testJobs;
    Set<Image> availableImages;
    Image baseImage;
    CloudModel cloudModel;
    // Overwrites the value in cloudModel
    int nReservedInstances;
    Goal goal;
    //
    IResultProcessor resultProcessor;
    //
    String outputFile;
    //
    boolean stopOnError;

    boolean progress;

    public void parseArgs(String[] args) throws ParseException, YamlException, FileNotFoundException {
	Options options = new Options();
	// comma separated list of names
	options.addOption(OptionBuilder.withLongOpt("planners").isRequired().hasArg()// .withValueSeparator(',')
		.withArgName("comma-separated planner names").create('p'));

	// YAML File
	options.addOption(
		OptionBuilder.withLongOpt("test-jobs").isRequired().hasArg().withArgName("YAML file").create('t'));

	// YAML File
	options.addOption(OptionBuilder.withLongOpt("available-images").isRequired().hasArg().withArgName("YAML file")
		.create('i'));

	// ID/Name of the base image - must be in the available images !
	options.addOption(
		OptionBuilder.withLongOpt("base-image-id").hasArg().withArgName("Image id or name").create('b'));
	// YAML File
	options.addOption(
		OptionBuilder.withLongOpt("cloud-model").isRequired().hasArg().withArgName("YAML file").create('c'));

	// YAML File
	options.addOption(OptionBuilder.withLongOpt("goal").isRequired().hasArg().withArgName("YAML file").create('g'));

	// Additionally specify out to process results: Class name
	options.addOption(OptionBuilder.withLongOpt("result-processor").hasArg().withArgName("Class QFN").create('R'));

	// Additionally specify out to process results: Class name
	options.addOption(OptionBuilder.withLongOpt("output-file").hasArg()
		.withArgName("Store Result to file a Serialized object").create('o'));

	//
	options.addOption(
		OptionBuilder.withLongOpt("n-reserved-instances").hasArg().withArgName("INTEGER").create('r'));

	options.addOption(OptionBuilder.withLongOpt("show-progress").create('p'));

	options.addOption(
		OptionBuilder.withLongOpt("n-reserved-instances").hasArg().withArgName("INTEGER").create('r'));

	options.addOption(OptionBuilder.withLongOpt("stop-on-error").withArgName("Flag").create('e'));
	//
	// INCLUDE Java Properties as well - Not needed! Just export the
	// JAVA_OPTS property and it will be fine !
	options.addOption(OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator()
		.withDescription("use value for given property").create("D"));

	CommandLineParser parser = new BasicParser();
	CommandLine line = null;
	// parse the command line arguments
	line = parser.parse(options, args);

	stopOnError = line.hasOption('e');

	this.progress = line.hasOption('p');
	// Setting Java Options - read them 2 by 2 !
	if (line.hasOption('D')) {
	    String javaOpts[] = line.getOptionValues('D');
	    for (int i = 0; i < javaOpts.length; i = i + 2) {
		System.err.println("Setting : " + javaOpts[i] + "=" + javaOpts[i + 1]);
		System.setProperty(javaOpts[i], javaOpts[i + 1]);
	    }

	}

	// Validating input

	File availableImageFile = new File(line.getOptionValue('i'));
	YamlReader availableImageReader = new YamlReader(new FileReader(availableImageFile));
	this.availableImages = availableImageReader.read(Set.class, Image.class);

	if (line.hasOption('b')) {
	    final String baseImageNameOrId = line.getOptionValue('b');
	    for (Image image : this.availableImages) {
		if (baseImageNameOrId.equals(image.name) || baseImageNameOrId.equals(image.getId())) {
		    this.baseImage = image;
		    break;
		}
	    }
	}

	if (this.baseImage == null) {
	    logger.debug("Using default base image");
	    this.baseImage = Image.getEmptyImage();
	}

	// TODO This can be improved
	for (String plannerName : line.getOptionValue('p').split(",")) {
	    try {
		Class<?> plannerClass = this.getClass().forName(plannerName);
		// Try to instantiate the planner - Must provide no-args
		// constructor !
		this.planners.add((TestExecutionPlanner) plannerClass.getConstructor().newInstance());
	    } catch (ClassCastException | ClassNotFoundException | InstantiationException | IllegalAccessException
		    | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
		    | SecurityException e) {
		logger.warn("Error creating planner " + plannerName + " " + e);
		// e.printStackTrace();
	    }
	}

	File testJobsFile = new File(line.getOptionValue('t'));
	YamlReader testJobsReader = new YamlReader(new FileReader(testJobsFile));
	// Set or HashSet ?!
	this.testJobs = testJobsReader.read(Set.class, TestJob.class);

	File cloudModelFile = new File(line.getOptionValue('c'));
	YamlReader cloudModelReader = new YamlReader(new FileReader(cloudModelFile));
	this.cloudModel = cloudModelReader.read(CloudModel.class);

	if (line.hasOption('r')) {
	    nReservedInstances = Integer.parseInt(line.getOptionValue('r'));
	    // TODO Validation ?
	    this.cloudModel.nReservedInstances = nReservedInstances;
	}

	File goalFile = new File(line.getOptionValue('g'));
	YamlReader goalReader = new YamlReader(new FileReader(goalFile));
	this.goal = goalReader.read(Goal.class);

	//
	if (line.hasOption('R')) {
	    Class<?> resultProcessorClass;
	    try {
		resultProcessorClass = this.getClass().forName(line.getOptionValue('R').trim());
		this.resultProcessor = (IResultProcessor) (resultProcessorClass.getConstructor().newInstance());
	    } catch (ClassCastException | ClassNotFoundException | InstantiationException | IllegalAccessException
		    | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
		    | SecurityException e) {
		logger.warn("Error creating result processor " + line.getOptionValue('R') + " " + e);
	    }
	} //
	if (this.resultProcessor == null) {
	    this.resultProcessor = new StdOutResultProcessor();
	}

	if (line.hasOption('o')) {
	    this.outputFile = line.getOptionValue('o');
	}
    }

    public Result execute() {
	// Result result = new Result(this.goal);
	Result result = new Result();
	result.availableImages = this.availableImages;
	for (TestExecutionPlanner planner : planners) {
	    result.planners.add(planner.getName());
	    try {
		logger.debug("EvaluationDriver.execute() Starting Execution of: " + planner);
		//
		long startTime = System.currentTimeMillis();
		if (this.progress) {
		    System.out.println("Starting Execution of: " + planner + " at " + new DateTime(startTime));

		}
		result.schedules.put(planner.getName(),
			planner.computeSchedule(testJobs, availableImages, this.baseImage, cloudModel, goal));
		if (this.progress) {
		    long endTime = System.currentTimeMillis();
		    System.out.println("End Execution of: " + planner + " at " + new DateTime(endTime));
		    System.out.println("After " + (endTime - startTime) + " msec");
		}
	    } catch (Throwable e) {
		e.printStackTrace();
		result.schedules.put(planner.getName(), Schedule.ERROR_SCHEDULE);
		if (stopOnError) {
		    throw e;
		}
	    } finally {
		logger.debug("EvaluationDriver.execute() Ending Execution of: " + planner + " result:\n"
			+ result.schedules.get(planner));
	    }
	}
	return result;
    }

    public void process(Result result) {
	if (this.outputFile != null) {
	    result.toFile(this.outputFile);
	}

	// Defensive anyway -
	if (this.resultProcessor != null) {
	    this.resultProcessor.process(result);
	} else {
	    new StdOutResultProcessor().process(result);
	}

    }

    public static void main(String[] args) throws FileNotFoundException, YamlException, ParseException {

	EvaluationDriver driver = new EvaluationDriver();
	driver.parseArgs(args);
	// Start the actual execution
	Result result = driver.execute();
	// Default Output to Console ?
	driver.process(result);
    }

}