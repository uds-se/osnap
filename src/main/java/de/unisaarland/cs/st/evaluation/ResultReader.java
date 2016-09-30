package de.unisaarland.cs.st.evaluation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.esotericsoftware.yamlbeans.YamlException;

import de.unisaarland.cs.st.evaluation.resultprocessors.StdOutResultProcessor;

public class ResultReader {
    private final static Logger logger = Logger.getLogger(ResultReader.class);
    //
    IResultProcessor resultProcessor;
    //
    String[] inputFiles;

    public void parseArgs(String[] args) throws ParseException, YamlException, FileNotFoundException {
	Options options = new Options();
	// space separated list of names
	options.addOption(OptionBuilder.withLongOpt("input-file").isRequired().hasArgs().withValueSeparator(' ')
		.withArgName("Input file storing serialized result object(s)").create('i'));

	// Specify how to process results: Class name
	options.addOption(OptionBuilder.withLongOpt("result-processor").hasArg().withArgName("Class QFN").create('R'));

	CommandLineParser parser = new BasicParser();
	CommandLine line = null;
	// parse the command line arguments
	line = parser.parse(options, args);

	// Validating input

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

	if (line.hasOption('i')) {
	    this.inputFiles = line.getOptionValues('i');
	}
    }

    public void execute() throws FileNotFoundException, ClassNotFoundException, IOException {
	// Repeat the same for each input file !
	for (String inputFile : inputFiles) {
	    Result result = Result.fromFile(inputFile);
	    // Defensive anyway -
	    if (this.resultProcessor != null) {
		this.resultProcessor.process(result);
	    } else {
		new StdOutResultProcessor().process(result);
	    }
	}

    }

    public static void main(String[] args)
	    throws FileNotFoundException, ClassNotFoundException, IOException, ParseException {

	ResultReader reader = new ResultReader();
	reader.parseArgs(args);
	// Read and process reader
	reader.execute();
    }
}
