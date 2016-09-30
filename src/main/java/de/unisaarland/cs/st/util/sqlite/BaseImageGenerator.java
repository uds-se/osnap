package de.unisaarland.cs.st.util.sqlite;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.esotericsoftware.yamlbeans.YamlWriter;

import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Package;

/**
 * Queries a sqlite DB containg info about debci tests and packages and creates
 * YAML files of the Debian Base Image at a given submission_at time (which is
 * one of the available snapshots at debian).
 * 
 * @author gambi
 *
 */
public class BaseImageGenerator {

    private String connection;

    // Testing
    public void setDbName(String dbName) throws ClassNotFoundException {
	this.dbName = dbName;
	this.connection = "jdbc:sqlite:" + this.dbName;
	Class.forName("org.sqlite.JDBC");
    }

    public BaseImageGenerator() {
    }

    private final String BASE_IMAGE_QUERY = "SELECT P.name, P.version, P.download_time, P.install_time FROM BaseImageConf as BC, Package as P WHERE BC.submit_at='%s' AND BC.name=P.name AND BC.version=P.version;";

    //
    private String dbName;
    private String submitAt;
    private String yamlOutputFile;
    //
    private boolean outputAsSet;

    public void parseArgs(String[] args) throws ParseException {
	Options options = new Options();

	// DB Name
	options.addOption(OptionBuilder.withLongOpt("db-name").isRequired().hasArg()//
		.withArgName("Sqlite DB").create('d'));

	// Submit At TODO Not checked !
	options.addOption(OptionBuilder.withLongOpt("submit-at").isRequired().hasArg()//
		.withArgName("Submit At").create('s'));

	// Output File
	options.addOption(OptionBuilder.withLongOpt("output-file").hasArg()//
		.withArgName("Output File").create('o'));

	// Output as Set instead - not mandatory only long option
	options.addOption(
		OptionBuilder.withArgName("Output a set of images ").withLongOpt("output-as-set").create('O'));

	CommandLineParser parser = new BasicParser();
	CommandLine line = null;
	// parse the command line arguments
	line = parser.parse(options, args);

	this.dbName = line.getOptionValue('d');
	this.submitAt = line.getOptionValue('s');
	if (line.hasOption('o')) {
	    this.yamlOutputFile = line.getOptionValue('o');
	}
	this.outputAsSet = line.hasOption("output-as-set");
    }

    public Set<Package> getBaseImagePackages(String _submitAt) {
	Set<Package> packages = new HashSet<Package>();
	try (Connection c = DriverManager.getConnection(connection)) {
	    c.setAutoCommit(false);
	    Statement stmt = null;
	    ResultSet rs = null;

	    stmt = c.createStatement();
	    // System.err.println("Executing: " +
	    // String.format(BASE_IMAGE_QUERY, _submitAt));
	    rs = stmt.executeQuery(String.format(BASE_IMAGE_QUERY, _submitAt));
	    while (rs.next()) {

		String name = rs.getString("name").replaceAll("'", "").trim();
		String version = rs.getString("version").replaceAll("'", "").trim();
		int downloadTime = rs.getInt("download_time");
		int installationTime = rs.getInt("install_time");

		// This has only basic data. Info on timing will be queried
		// later
		Package baseImagePackage = new Package();
		baseImagePackage.name = name;
		baseImagePackage.version = version;
		baseImagePackage.downloadTime = downloadTime;
		baseImagePackage.installationTime = installationTime;
		//
		packages.add(baseImagePackage);
	    }
	    rs.close();
	    stmt.close();

	} catch (Exception e) {
	    System.err.println("Error in generating base image for " + submitAt);
	    e.printStackTrace();
	    // System.err.println(e.getClass().getName() + ": " +
	    // e.getMessage());
	}
	return packages;
    }

    public void generateBaseImage() throws ClassNotFoundException, IOException {
	setDbName(this.dbName);
	// Output
	Set<Package> baseImagePackages = getBaseImagePackages(this.submitAt);

	// Build the Image Object
	Image baseImage = new Image();
	baseImage.parentImage = Image.getEmptyImage();
	baseImage.installedPackages = baseImagePackages;
	// Not necessary but Ok
	baseImage.name = this.submitAt;

	Writer outputWriter = null;

	if (this.yamlOutputFile != null) {
	    // Print to File
	    File output = new File(this.yamlOutputFile);
	    if (output.getParentFile() != null && !output.getParentFile().exists())
		output.getParentFile().mkdirs();
	    outputWriter = new FileWriter(output);
	} else {
	    // Print to Console
	    outputWriter = new PrintWriter(System.out);
	}

	YamlWriter yamlWriter = new YamlWriter(outputWriter);
	yamlWriter.write(baseImage);
	yamlWriter.close();
    }

    public void generateBaseImageAsSet() throws ClassNotFoundException, IOException {
	setDbName(this.dbName);
	// Output
	Set<Package> baseImagePackages = getBaseImagePackages(this.submitAt);

	// Build the Image Object
	Image emptyImage = Image.getEmptyImage();
	;
	Image baseImage = new Image();
	baseImage.parentImage = emptyImage;
	baseImage.installedPackages = baseImagePackages;
	// Not necessary but Ok
	baseImage.name = this.submitAt;

	Set<Image> availableImages = new HashSet<Image>();
	availableImages.add(emptyImage);
	availableImages.add(baseImage);

	Writer outputWriter = null;

	if (this.yamlOutputFile != null) {
	    // Print to File
	    File output = new File(this.yamlOutputFile);
	    if (output.getParentFile() != null && !output.getParentFile().exists())
		output.getParentFile().mkdirs();
	    outputWriter = new FileWriter(output);
	} else {
	    // Print to Console
	    outputWriter = new PrintWriter(System.out);
	}

	YamlWriter yamlWriter = new YamlWriter(outputWriter);
	yamlWriter.write(availableImages);
	yamlWriter.close();
    }

    public void execute() throws ClassNotFoundException, IOException {

	if (outputAsSet) {
	    generateBaseImageAsSet();
	} else {
	    generateBaseImage();
	}

    }

    // LIST OF TEST JOBS (NAME, VERSION, SUBMIT_AT) --> FILE YAML
    public static void main(String[] args) throws ParseException, ClassNotFoundException, IOException {
	BaseImageGenerator tjg = new BaseImageGenerator();
	tjg.parseArgs(args);
	//
	tjg.execute();

    }

}
