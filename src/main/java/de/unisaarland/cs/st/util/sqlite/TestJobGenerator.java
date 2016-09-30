package de.unisaarland.cs.st.util.sqlite;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.esotericsoftware.yamlbeans.YamlWriter;

import de.unisaarland.cs.st.data.Package;
import de.unisaarland.cs.st.data.TestJob;

/**
 * Queries a sqlite DB containg info about debci tests and packages and creates
 * YAML files for them.
 * 
 * 
 * We filter out test jobs that have very large execution times (12h+)
 * 
 * We filter out test jobs which miss informations, including packages that they
 * depend upon.
 * 
 * @author gambi
 *
 */
/*
 * Sqlite DB Schema:
 * 
 * CREATE TABLE TestJob (name STRING, version STRING, submit_at INTEGER,
 * start_at INTEGER, end_at INTEGER, reason STRING, PRIMARY KEY(name, version,
 * submit_at)); CREATE TABLE Dependency (name STRING, version STRING, submit_at
 * INTEGER, depends_on_name STRING, depends_on_version STRING); CREATE TABLE
 * Package (name STRING, version STRING, download_time INTEGER, install_time
 * INTEGER, PRIMARY KEY(name, version));
 *
 */

public class TestJobGenerator {

    private final Logger logger = Logger.getLogger(TestJobGenerator.class);

    // This was found "empirically" Over those we do not consider a test job
    public static final long MAX_DURATION = 4 * 60 * 60;

    public static final String DB_FOLDER = "/Users/gambi/ASE2016/dataset";
    public static final String DB_NAME = "MainDataSet.db";

    private final String queryCountTestJob = "select count(*) from TestJob where submit_at >= %s and submit_at < %s;";

    private final String queryTestJobByPackageNameAndVersion = "select name, version, submit_at, start_at, end_at from TestJob where name='%s' and version='%s';";

    // This removes automatically empty packages
    private final String queryTestJobDependencies = "select P.name, P.version, P.download_time, P.install_time"
	    + " from Package as P, Dependency as D"
	    + " where P.name=D.depends_on_name and P.version=D.depends_on_version and D.name='%s' and D.version='%s' and D.submit_at='%s'"
	    + " and P.download_time is not null and P.install_time is not null"//
	    + ";";

    private final String allDependencies = "SELECT P.name, P.version, P.download_time, P.install_time"
	    + " FROM Package as P, Dependency as D"
	    + " WHERE P.name=D.depends_on_name and P.version=D.depends_on_version and D.name='%s' and D.version='%s' and D.submit_at='%s';";

    // = "select depends_on_name, depends_on_version from Dependency where
    // name=\"%s\" and version=\"%s\" and submit_at=\"%s\";";

    private String connection;

    // public TestJobGenerator(String dbFile) throws ClassNotFoundException {
    // this.connection = "jdbc:sqlite:" + dbFile;
    // Class.forName("org.sqlite.JDBC");
    // }

    // Testing
    public void setDbName(String dbName) throws ClassNotFoundException {
	this.dbName = dbName;
	this.connection = "jdbc:sqlite:" + this.dbName;
	Class.forName("org.sqlite.JDBC");
    }

    public TestJobGenerator() {
    }

    /**
     * Use for DEBIAN CI that runs every 3 hours
     * 
     * @param startDate
     * @param endDate
     * @return
     * @throws ClassNotFoundException
     */
    public List<Entry<DateTime, Set<TestJob>>> everyThreeHours(DateTime startDate, DateTime endDate)
	    throws ClassNotFoundException {
	DateTime startSlot = startDate.withTimeAtStartOfDay();
	DateTime endDay = endDate.withTimeAtStartOfDay();
	//
	DateTime theNext3Hours = startSlot.plusHours(3);
	//
	List<Entry<DateTime, Set<TestJob>>> result = new ArrayList<Entry<DateTime, Set<TestJob>>>();

	while (theNext3Hours.isBefore(endDay) || theNext3Hours.minusHours(3).isBefore(endDay)) {

	    String theQuery = String.format(QUERY_TEST_JOBS_BETWEEN_DATES, //
		    startSlot.getMillis() / 1000, //
		    theNext3Hours.getMillis() / 1000);

	    result.add(new AbstractMap.SimpleEntry<DateTime, Set<TestJob>>(startSlot, getTestJobsSubmitted(theQuery)));
	    //
	    startSlot = theNext3Hours;
	    theNext3Hours = startSlot.plusHours(3);
	}
	Collections.sort(result, comparator);

	return result;

    }

    public List<Entry<DateTime, Set<TestJob>>> everyThreeHoursSMALL(DateTime startDate, DateTime endDate)
	    throws ClassNotFoundException {
	DateTime startSlot = startDate.withTimeAtStartOfDay();
	DateTime endDay = endDate.withTimeAtStartOfDay();
	//
	DateTime theNext3Hours = startSlot.plusHours(3);
	//
	List<Entry<DateTime, Set<TestJob>>> result = new ArrayList<Entry<DateTime, Set<TestJob>>>();

	while (theNext3Hours.isBefore(endDay) || theNext3Hours.minusHours(3).isBefore(endDay)) {

	    String theQuery = String.format(QUERY_TEST_JOBS_BETWEEN_DATES, //
		    startSlot.getMillis() / 1000, //
		    theNext3Hours.getMillis() / 1000);

	    String theCoutingQuery = String.format(queryCountTestJob, //
		    startSlot.getMillis() / 1000, //
		    theNext3Hours.getMillis() / 1000);

	    result.add(new AbstractMap.SimpleEntry<DateTime, Set<TestJob>>(startSlot,
		    getTestJobsSubmittedSMALL(theQuery, theCoutingQuery)));
	    //
	    startSlot = theNext3Hours;
	    theNext3Hours = startSlot.plusHours(3);
	}
	Collections.sort(result, comparator);

	return result;

    }

    /**
     * Use for SNAPSHOT DEBIAN that runs every 6 hours
     * 
     * @param startDate
     * @param endDate
     * @return
     * @throws ClassNotFoundException
     */
    public List<Entry<DateTime, Set<TestJob>>> everySixHours(DateTime startDate, DateTime endDate)
	    throws ClassNotFoundException {
	DateTime startSlot = startDate.withTimeAtStartOfDay();
	DateTime endDay = endDate.withTimeAtStartOfDay();
	//
	DateTime theNext6Hours = startSlot.plusHours(6);
	//
	List<Entry<DateTime, Set<TestJob>>> result = new ArrayList<Entry<DateTime, Set<TestJob>>>();

	while (theNext6Hours.isBefore(endDay) || theNext6Hours.minusHours(3).isBefore(endDay)) {

	    String theQuery = String.format(QUERY_TEST_JOBS_BETWEEN_DATES, //
		    startSlot.getMillis() / 1000, //
		    theNext6Hours.getMillis() / 1000);

	    result.add(new AbstractMap.SimpleEntry<DateTime, Set<TestJob>>(startSlot, getTestJobsSubmitted(theQuery)));
	    //
	    startSlot = theNext6Hours;
	    theNext6Hours = startSlot.plusHours(3);
	}
	Collections.sort(result, comparator);

	return result;

    }

    public List<Entry<DateTime, Set<TestJob>>> byPackageName(String packageName) throws ClassNotFoundException {
	List<Entry<DateTime, Set<TestJob>>> result = new ArrayList<Entry<DateTime, Set<TestJob>>>();

	String theQuery = String
		.format("select name, version, submit_at, start_at, end_at from TestJob where name='%s';", packageName);

	// This is ugly
	for (TestJob testjob : getTestJobsSubmitted(theQuery)) {
	    Set<TestJob> tj = new HashSet<TestJob>();
	    tj.add(testjob);
	    result.add(new AbstractMap.SimpleEntry<DateTime, Set<TestJob>>(new DateTime(testjob.submissionTime * 1000),
		    tj));
	}

	//
	Collections.sort(result, comparator);

	return result;

    }

    public List<Entry<DateTime, Set<TestJob>>> byPackageNameAndVersion(String packageName, String packageVersion)
	    throws ClassNotFoundException {
	List<Entry<DateTime, Set<TestJob>>> result = new ArrayList<Entry<DateTime, Set<TestJob>>>();
	String theQuery = String.format(queryTestJobByPackageNameAndVersion, packageName, packageVersion);

	// This is ugly
	for (TestJob testjob : getTestJobsSubmitted(theQuery)) {
	    Set<TestJob> tj = new HashSet<TestJob>();
	    tj.add(testjob);
	    result.add(new AbstractMap.SimpleEntry<DateTime, Set<TestJob>>(new DateTime(testjob.submissionTime * 1000),
		    tj));
	}

	//
	Collections.sort(result, comparator);

	return result;

    }

    private final Comparator<Entry<DateTime, Set<TestJob>>> comparator = new Comparator<Entry<DateTime, Set<TestJob>>>() {

	@Override
	public int compare(Entry<DateTime, Set<TestJob>> o1, Entry<DateTime, Set<TestJob>> o2) {
	    return Long.signum(o1.getKey().getMillis() - o1.getKey().getMillis());
	}
    };

    public List<Entry<DateTime, Set<TestJob>>> everyWeek(int howMany, DateTime startDate, DateTime endDate,
	    String packageName) throws ClassNotFoundException {
	// TODO DO not cut at the beginning of the day !
	DateTime startSlot = startDate;// .withTimeAtStartOfDay();
	DateTime endDay = endDate;// .withTimeAtStartOfDay();
	//
	DateTime theNextWeek = startSlot.plusWeeks(howMany);
	//
	List<Entry<DateTime, Set<TestJob>>> result = new ArrayList<Entry<DateTime, Set<TestJob>>>();
	while (theNextWeek.isBefore(endDay) || theNextWeek.minusDays(1).isBefore(endDay)) {

	    String theQuery = String.format(QUERY_TEST_JOBS_BETWEEN_DATES, //
		    startSlot.getMillis() / 1000, //
		    theNextWeek.getMillis() / 1000, packageName);

	    result.add(new AbstractMap.SimpleEntry<DateTime, Set<TestJob>>(startSlot, getTestJobsSubmitted(theQuery)));
	    //
	    startSlot = theNextWeek;
	    theNextWeek = startSlot.plusWeeks(howMany);
	}

	Collections.sort(result, comparator);

	return result;

    }

    public Set<TestJob> updateTestJobs(Set<TestJob> testJobs) throws ClassNotFoundException {
	for (TestJob testJob : testJobs) {
	    updateTestJobDependencies(testJob);
	    if (testJob.getAllPackages().size() == 0) {
		System.err.println("\n\n\n\n TestJobGenerator.updateTestJobs() WARN : No deps for " + testJob.sut);
	    }
	}
	return testJobs;
    }

    /**
     * Update the Object by filling in all its dependencies. Skip invalid
     * dependencies !
     * 
     * @param testJob
     * @return
     */
    public TestJob updateTestJobDependencies(TestJob testJob) {
	logger.debug("TestJobGenerator.updateTestJobDependencies() of " + testJob.sut + " " + testJob.submissionTime
		+ " with " + testJob.sut.dependencies.size() + " deps. Using query: \n"
		+ String.format(allDependencies, testJob.sut.name, testJob.sut.version, testJob.submissionTime));
	//
	try (Connection c = DriverManager.getConnection(connection)) {
	    c.setAutoCommit(false);
	    Statement stmt = null;
	    ResultSet rs = null;

	    stmt = c.createStatement();

	    rs = stmt.executeQuery(
		    String.format(allDependencies, testJob.sut.name, testJob.sut.version, testJob.submissionTime));

	    while (rs.next()) {
		// Try to clean up the data
		String name = rs.getString("name").replaceAll("'", "").trim();
		String version = rs.getString("version").replaceAll("'", "").trim();
		// Those are millisec
		int downloadTime = rs.getInt("download_time");
		int installationTime = rs.getInt("install_time");
		// Check validity and SKIP the package
		if (downloadTime < 0) {
		    logger.error(" Invalid download time for " + name + " " + version + ". Too small" + downloadTime);
		    continue;
		} else if (TimeUnit.MILLISECONDS.toHours(downloadTime) > 5) {
		    logger.error(" Invalid download time for " + name + " " + version + ". Too big" + downloadTime);
		    continue;
		}
		//
		if (installationTime < 0) {
		    logger.error(" Invalid installationTime time for " + name + " " + version + ". Too small"
			    + installationTime);
		    continue;
		} else if (TimeUnit.MILLISECONDS.toHours(installationTime) > 5) {
		    logger.error(" Invalid installationTime time for " + name + " " + version + ". Too big"
			    + installationTime);
		    continue;
		}

		//
		Package p = new Package();
		p.name = name;
		p.version = version;
		p.downloadTime = downloadTime;
		p.installationTime = installationTime;
		// Note that this might be inaccurate but for the moment is just
		// fine !
		testJob.sut.dependencies.add(p);
		// if (!) {
		// System.err.println("TestJobGenerator.updateTestJobDependencies()
		// Warning dep " + p.name + " "
		// + p.version + " was not added. Already present ? " +
		// testJob.sut.dependencies.contains(p));
		// }
		//// MOVE TO TRACE
		// else {
		// System.err.println(
		// "TestJobGenerator.updateTestJobDependencies() Adding dep : "
		// + p.name + " " + p.version);
		// }
	    }
	    //
	    rs.close();
	    stmt.close();
	} catch (Exception e) {
	    System.err.println(e.getClass().getName() + ": " + e.getMessage());
	}

	// Move to DEBUG :
	// System.err.println("TestJobGenerator.updateTestJobDependencies() of "
	// + testJob.sut + " has "
	// + testJob.sut.dependencies.size() + " dependencies");
	return testJob;
    }

    public Set<TestJob> getTestJobsSubmitted(String theQuery) throws ClassNotFoundException {
	// System.err.println("TestJobGenerator.getTestJobsSubmitted() " +
	// theQuery);
	Set<TestJob> testJobs = new HashSet<TestJob>();

	try (Connection c = DriverManager.getConnection(connection)) {
	    c.setAutoCommit(false);
	    Statement stmt = null;
	    ResultSet rs = null;

	    stmt = c.createStatement();
	    rs = stmt.executeQuery(theQuery);
	    System.err.println("--------------------------------------------------");
	    while (rs.next()) {
		String name = rs.getString("name").replaceAll("'", "").trim();
		String version = rs.getString("version").replaceAll("'", "").trim();
		//
		long submitAt = rs.getLong("submit_at");
		long duration = rs.getLong("end_at") - rs.getLong("start_at");
		//
		Package sut = new Package();
		sut.name = name;
		sut.version = version;
		//
		TestJob testJob = new TestJob(); // Automatic ID. Bad !
		testJob.sut = sut;
		testJob.submissionTime = submitAt;
		testJob.testDuration = duration;

		// TODO Safety check
		// System.err.println(String.format("%s|%s|%s", name, version,
		// submitAt));

		if (testJob.testDuration <= MAX_DURATION) {
		    if (!testJobs.add(testJob)) {
			System.err.println(
				"WARNING " + String.format("%s|%s|%s", name, version, submitAt) + " ALREADY ADDED ?! ");
		    }
		} else {
		    System.err.println("Test Job " + testJob.sut + " filtered out. Duration is too large " + duration);
		}

		System.err.println(String.format("%s %s %s", name, version, submitAt));

	    }
	    rs.close();
	    stmt.close();

	    System.err.println("--------------------------------------------------");
	} catch (Exception e) {
	    System.err.println(e.getClass().getName() + ": " + e.getMessage());
	}
	if (!this.onlySUT) {
	    updateTestJobs(testJobs);
	}

	return testJobs;
    }

    /**
     * Like the other but limit to 1/2 of the size
     * 
     * @param theQuery
     * @return
     * @throws ClassNotFoundException
     */
    public Set<TestJob> getTestJobsSubmittedSMALL(String theQuery, String countQuery) throws ClassNotFoundException {
	// System.err.println("TestJobGenerator.getTestJobsSubmitted() " +
	// theQuery);
	Set<TestJob> testJobs = new HashSet<TestJob>();

	try (Connection c = DriverManager.getConnection(connection)) {
	    c.setAutoCommit(false);
	    Statement stmt = null;
	    ResultSet rs = null;

	    //
	    stmt = c.createStatement();
	    rs = stmt.executeQuery(countQuery);
	    rs.next();
	    int size = rs.getInt(1);
	    System.err.println("TestJobGenerator.getTestJobsSubmittedSMALL() ORIGINAL SIZE = " + size);
	    if (size > 400)
		size = size / 4;
	    if (size > 300)
		size = size / 3;
	    else if (size > 100)
		size = size / 2;
	    System.err.println("TestJobGenerator.getTestJobsSubmittedSMALL() NEW SIZE = " + size);
	    rs.close();
	    stmt.close();
	    //
	    //
	    stmt = c.createStatement();
	    rs = stmt.executeQuery(theQuery);
	    System.err.println("--------------------------------------------------");
	    int i = 0;
	    while (rs.next()) {
		//
		i = i + 1;
		//
		String name = rs.getString("name").replaceAll("'", "").trim();
		String version = rs.getString("version").replaceAll("'", "").trim();
		//
		long submitAt = rs.getLong("submit_at");
		long duration = rs.getLong("end_at") - rs.getLong("start_at");
		//
		Package sut = new Package();
		sut.name = name;
		sut.version = version;
		//
		TestJob testJob = new TestJob(); // Automatic ID. Bad !
		testJob.sut = sut;
		testJob.submissionTime = submitAt;
		testJob.testDuration = duration;

		// TODO Safety check
		// System.err.println(String.format("%s|%s|%s", name, version,
		// submitAt));

		if (testJob.testDuration <= MAX_DURATION) {
		    if (!testJobs.contains(testJob) && !testJobs.add(testJob)) {
			System.err.println(
				"WARNING " + String.format("%s|%s|%s", name, version, submitAt) + " NOT ADDED ");
		    }
		} else {
		    System.err.println("Test Job " + testJob.sut + " filtered out. Duration is too large " + duration);
		}

		System.err.println(String.format("%s %s %s", name, version, submitAt));

		if (i >= size) {
		    break;
		}

	    }
	    rs.close();
	    stmt.close();

	    System.err.println("--------------------------------------------------");
	} catch (Exception e) {
	    System.err.println(e.getClass().getName() + ": " + e.getMessage());
	}

	// System.err.println("TestJobGenerator.getTestJobsSubmitted() GOT " +
	// testJobs.size() + " entries");

	updateTestJobs(testJobs);

	return testJobs;
    }

    public final static String[] KDE_FAMILY = new String[] { "kde-baseapps", "kdelibs4support", "kdepim", "kdepimlibs",
	    "libkdegames-kde4", "plasma-workspace", };

    public final static String[] TOP10 = new String[] { "libreoffice", "cinnamon-control-center", "kde-baseapps",
	    "kajongg", "parley", "kate", "gwenview", "kopete", "knetwalk", "okular" };

    private static void topTen() throws ClassNotFoundException, IOException {

	String[] allPackages = new String[TOP10.length];
	System.arraycopy(TOP10, 0, allPackages, 0, TOP10.length);

	for (int i = 0; i < allPackages.length; i = i + 1) {

	    // get all the versions

	    // Process all the version !

	    String packageName = allPackages[i];
	    System.err.println("TestJobGenerator.topTen(): Processing " + packageName);

	    //
	    String byPackageFolder = dataFolder + "/by-package/" + packageName;

	    DateTimeFormatter dateformatter = DateTimeFormat.forPattern("yyyyMMdd_HHmmss");

	    // Read from standard DB and generate to standard folders
	    TestJobGenerator tg = new TestJobGenerator(); // TheDbFile);

	    // Print to file interesting metadata
	    String dataFileName = byPackageFolder + "/timeseries_from_" + TheDbName + ".data";
	    File dataFile = new File(dataFileName);

	    if (!dataFile.getParentFile().exists()) {
		dataFile.getParentFile().mkdirs();
	    }

	    PrintWriter dataFileWriter = new PrintWriter(dataFile);
	    //
	    // Print out also the number of test jobs submitted in each period
	    dataFileWriter.println("# FileName | SubmissionTime | N Test Jobs");

	    System.err.println("TestJobGenerator.main() everyThreeHours ");
	    // Order by time !
	    for (Entry<DateTime, Set<TestJob>> result : tg.byPackageName(packageName)) {

		if (result.getValue().size() == 0)
		    continue;

		// Generating output files !
		String ouputFileName = byPackageFolder + "/" + dateformatter.print(result.getKey()) + ".yml";
		//
		File output = new File(ouputFileName);
		output.getParentFile().mkdirs();
		YamlWriter yamlWriter = new YamlWriter(new FileWriter(output));
		yamlWriter.write((HashSet<TestJob>) result.getValue());
		yamlWriter.close();
		// System.err.println("TestJobGenerator.main() done with " +
		// output.getAbsolutePath());
		dataFileWriter.println(String.format("%s %d %d", ouputFileName, (result.getKey().getMillis() / 1000),
			result.getValue().size()));
	    }
	    dataFileWriter.close();
	    System.err.println("TestJobGenerator.main() Done " + byPackageFolder);
	}
    }

    public List<Entry<DateTime, Set<TestJob>>> everyThreeHoursByPackageNames(DateTime startDate, DateTime endDate,
	    String[] packageNames) throws ClassNotFoundException {

	DateTime startSlot = startDate.withTimeAtStartOfDay();
	DateTime endDay = endDate.withTimeAtStartOfDay();
	//
	DateTime theNext3Hours = startSlot.plusHours(3);
	//
	List<Entry<DateTime, Set<TestJob>>> result = new ArrayList<Entry<DateTime, Set<TestJob>>>();

	StringBuffer nameOR = new StringBuffer();
	for (int i = 0; i < packageNames.length - 1; i++) {
	    nameOR.append(String.format("name='%s' OR ", packageNames[i]));
	}
	nameOR.append(String.format("name='%s'", packageNames[packageNames.length - 1]));

	while (theNext3Hours.isBefore(endDay) || theNext3Hours.minusHours(3).isBefore(endDay)) {

	    String theQuery = String.format(
		    "select name, version, submit_at, start_at, end_at from TestJob where submit_at >= %s and submit_at < %s AND ( %s );", //
		    startSlot.getMillis() / 1000, //
		    theNext3Hours.getMillis() / 1000, nameOR.toString());

	    // System.err.println("TestJobGenerator.everyThreeHoursByPackageNames()
	    // QUERY " + theQuery);

	    result.add(new AbstractMap.SimpleEntry<DateTime, Set<TestJob>>(startSlot, getTestJobsSubmitted(theQuery)));
	    //
	    startSlot = theNext3Hours;
	    theNext3Hours = startSlot.plusHours(3);
	}
	Collections.sort(result, comparator);

	return result;

    }

    public static final String dataFolder = "src/main/resources/de.unisaarland.cs.st/evaluation/test-jobs";
    public static String TheDbName = TestJobGenerator.DB_NAME;
    public static String TheDbFile = TestJobGenerator.DB_FOLDER + "/" + TheDbName;
    //
    private final String RANDOM_SAMPLE_QUERY = "SELECT * FROM TestJob WHERE submit_at='%s' ORDER BY RANDOM() LIMIT '%s';";

    private String dbName;
    private String submitAt;
    private String sampleSize;
    private String yamlOutputFile;
    // Name of packages to include if any
    private String[] include = new String[] {};

    private boolean onlySUT;

    private String[] groupBy; // Enum Day 1, Week 4, limit 2

    private String[] endAfter; // Used with groupBy, limit size 2

    public void parseArgs(String[] args) throws ParseException {
	Options options = new Options();
	// DB Name
	options.addOption(OptionBuilder.withLongOpt("db-name").isRequired().hasArg()//
		.withArgName("Sqlite DB").create('d'));

	// Submit At TODO Not checked !
	options.addOption(OptionBuilder.withLongOpt("submit-at").isRequired().hasArg()//
		.withArgName("Submit At").create('s'));

	// Sample File
	options.addOption(OptionBuilder.withLongOpt("sample-size").hasArg()//
		.withArgName("Sample Size").create('n'));

	// Output File
	options.addOption(OptionBuilder.withLongOpt("output-file").hasArg()//
		.withArgName("Output File").create('o'));

	// Name of test jobs to select from at the given time. Versions is
	// derived
	// If the size is not compatible with the include we select the first n
	// elements that belongs to include if n is smaller. If n is larger we
	// randomly select the others
	options.addOption(OptionBuilder.withLongOpt("include").hasArgs().withValueSeparator(' ')//
		.withArgName("Include ").create('i'));

	options.addOption(OptionBuilder.withLongOpt("group-by").hasArgs().withValueSeparator(' ').create('g'));

	options.addOption(OptionBuilder.withLongOpt("end-after").hasArgs().withValueSeparator(' ').create('e'));

	options.addOption(
		OptionBuilder.withLongOpt("output-only-sut").withArgName("Output only data about SUT").create('O'));

	CommandLineParser parser = new BasicParser();
	CommandLine line = null;
	// parse the command line arguments
	line = parser.parse(options, args);

	this.dbName = line.getOptionValue('d');
	this.submitAt = line.getOptionValue('s');

	// Note that this is not required anymore ! If include is specified
	if (line.hasOption('n')) {
	    this.sampleSize = line.getOptionValue('n');
	}

	if (line.hasOption('o')) {
	    this.yamlOutputFile = line.getOptionValue('o');
	}
	if (line.hasOption('i')) {
	    this.include = line.getOptionValues('i');
	    if (this.sampleSize == null)
		this.sampleSize = "" + this.include.length;
	}

	this.onlySUT = line.hasOption('O');

	if (line.hasOption('g')) {
	    this.groupBy = line.getOptionValues('g');

	    if (line.hasOption('e'))
		this.endAfter = line.getOptionValues('e');
	}

	if (this.sampleSize == null)
	    throw new ParseException("Sample Size Null !!");

    }

    public Set<TestJob> getTestJobs(String _submitAt, String _sampleSize, String[] _include) throws SQLException {
	Set<TestJob> testJobs = new HashSet<TestJob>();
	try (Connection c = DriverManager.getConnection(connection); Statement stmt = c.createStatement();) {
	    c.setAutoCommit(false);
	    // Statement stmt = null;

	    // stmt = c.createStatement();
	    // System.err.println(
	    // "TestJobGenerator.generate() " +
	    // String.format(RANDOM_SAMPLE_QUERY, _submitAt, _sampleSize));

	    if (_include.length > 0) {
		StringBuffer namesCondition = new StringBuffer();
		for (int i = 0; i < _include.length; i++) {
		    if (i == 0)
			namesCondition.append(String.format("name='%s'", _include[i]));
		    else
			namesCondition.append(String.format(" OR name='%s'", _include[i]));

		}
		String includeQuery = String.format(
			"SELECT * FROM TestJob WHERE submit_at='%s' AND (%s) ORDER BY name ASC;", _submitAt,
			namesCondition.toString());

		logger.debug("Include Query " + includeQuery);
		// SELECT THE PACKAGES FROM INCLUDE FIRST IF ANY
		List<TestJob> includes = new ArrayList<TestJob>();
		try (ResultSet rs = stmt.executeQuery(includeQuery)) {
		    while (rs.next()) {
			String name = rs.getString("name").replaceAll("'", "").trim();
			String version = rs.getString("version").replaceAll("'", "").trim();
			//
			long submitAt = rs.getLong("submit_at");
			long duration = rs.getLong("end_at") - rs.getLong("start_at");
			//
			Package sut = new Package();
			sut.name = name;
			sut.version = version;
			// SUT must appear as dep of itself for download and
			// install
			// since we are testing it 'as installed'
			sut.downloadTime = Integer.MIN_VALUE;
			sut.installationTime = Integer.MIN_VALUE;
			//
			TestJob testJob = new TestJob(); // Automatic ID. Bad !
			testJob.sut = sut;
			testJob.submissionTime = submitAt;
			testJob.testDuration = duration;

			if (testJob.testDuration <= MAX_DURATION) {
			    if (!includes.add(testJob)) {
				logger.warn(String.format("*** %s|%s|%s", name, version, submitAt) + " NOT ADDED ");
			    }
			} else {
			    logger.warn(
				    "*** Test Job " + testJob.sut + " filtered out. Duration is too large " + duration);
			}

		    }
		} // rs. autocloses here
		  // Add as many packages as possible here. If they are not
		  // enough we will add later, if they are too much we stop at
		  // the size, otherwise we just add all of them
		int offset = 0;
		Iterator<TestJob> iterator = includes.iterator();
		while (offset < Integer.parseInt(_sampleSize) && iterator.hasNext()) {
		    testJobs.add(iterator.next());
		    offset++;
		}
	    }

	    // This creates problems. If include lenght and sample size lentght
	    // are the same we do not care about the actual instances that we
	    // found in the period. Basically we do not guarantee a given size
	    // if packages are missing...

	    // if (testJobs.size() < Integer.parseInt(_sampleSize)) {
	    if (_include.length < Integer.parseInt(_sampleSize)) {

		// int randomElements = Integer.parseInt(_sampleSize) -
		// testJobs.size();
		int randomElements = Integer.parseInt(_sampleSize) - _include.length;

		try (ResultSet rs = stmt
			.executeQuery(String.format(RANDOM_SAMPLE_QUERY, _submitAt, "" + randomElements));) {

		    logger.info("--------------------------------------------------");
		    while (rs.next()) {
			String name = rs.getString("name").replaceAll("'", "").trim();
			String version = rs.getString("version").replaceAll("'", "").trim();
			//
			long submitAt = rs.getLong("submit_at");
			long duration = rs.getLong("end_at") - rs.getLong("start_at");
			//
			Package sut = new Package();
			sut.name = name;
			sut.version = version;
			// SUT must appear as dep of itself for download and
			// install
			// since we are testing it 'as installed'
			sut.downloadTime = Integer.MIN_VALUE;
			sut.installationTime = Integer.MIN_VALUE;
			//
			TestJob testJob = new TestJob(); // Automatic ID. Bad !
			testJob.sut = sut;
			testJob.submissionTime = submitAt;
			testJob.testDuration = duration;

			// TODO Safety check
			logger.info((String.format("%s|%s|%s", name, version, submitAt)));

			if (testJob.testDuration <= MAX_DURATION) {

			    if (testJobs.contains(testJob)) {
				for (TestJob tj : testJobs) {
				    if (tj.equals(testJob)) {
					System.err.println(" ALREADY CONTAINS EQUALS " + tj + " -- " + testJob);
					System.err.println(tj.submissionTime + " " + testJob.submissionTime);
				    }
				    if (tj.hashCode() == testJob.hashCode())
					System.err.println(" ALREADY CONTAINS HASH " + tj + " -- " + testJob);
				}
			    }

			    if (!testJobs.contains(testJob) && !testJobs.add(testJob)) {
				logger.warn("WARNING " + String.format("%s|%s|%s", name, version, submitAt)
					+ " NOT ADDED ");
			    } else {

			    }
			} else {
			    logger.warn("Test Job " + testJob.sut + " filtered out. Duration is too large " + duration);
			}

			// System.err.println(String.format("%s %s %s", name,
			// version,
			// submitAt));

		    }
		    // rs.close();Autocloses
		    // stmt.close(); Autocloses

		    logger.info("--------------------------------------------------");
		}

	    }
	} // Connection autocloses
	  // System.err.println("--------------------------------------------------");
	  // TODO Misplaced catch ?
	return testJobs;
    }

    private DateTime computeEndDate(final DateTime startDate, final String timeunit, int howMany) {
	switch (timeunit) {
	case "week":
	    return startDate.plusWeeks(howMany);
	case "day":
	default:
	    return startDate.plusDays(howMany);
	}
    }

    // Enable testability
    public Object generate() throws ClassNotFoundException, IOException, SQLException {
	setDbName(this.dbName);

	if (this.groupBy == null) {

	    // Output
	    Set<TestJob> testJobs = getTestJobs(this.submitAt, this.sampleSize, this.include);

	    // System.err.println("TestJobGenerator.getTestJobsSubmitted() GOT "
	    // +
	    // testJobs.size() + " entries");
	    // TODO Check this !
	    if (!this.onlySUT) {
		updateTestJobs(testJobs);
	    }

	    Writer outputWriter = null;

	    if (this.yamlOutputFile != null) {
		// Print to File
		File output = new File(this.yamlOutputFile);
		// System.err.println("TestJobGenerator.generate() " +
		// this.yamlOutputFile);
		if (output.getParentFile() != null && !output.getParentFile().exists())
		    output.getParentFile().mkdirs();
		outputWriter = new FileWriter(output);
	    } else {
		// Print to Console
		outputWriter = new PrintWriter(System.out);
	    }

	    YamlWriter yamlWriter = new YamlWriter(outputWriter);
	    yamlWriter.write(testJobs);
	    yamlWriter.close();
	    return testJobs;
	} else {
	    // TODO: Add a suffix to these test jobs objects to account for
	    // their relative order !
	    for (String packageName : this.include) {
		// submit_at is in seconds
		DateTime startDate = new DateTime(Long.parseLong(this.submitAt) * 1000);
		DateTime endDate = new DateTime(getMaxSubmitAt() * 1000);

		List<Entry<DateTime, Set<TestJob>>> groupedTestJobs = new ArrayList<Entry<DateTime, Set<TestJob>>>();
		String classifier = "";
		int howManyStartDate = (this.groupBy.length > 1) ? Integer.parseInt(this.groupBy[1]) : 1;
		int howManyEndDate = (this.endAfter != null && this.endAfter.length > 1)
			? Integer.parseInt(this.endAfter[1]) : 1;

		// TODO Ideally here one shall compute the total seconds of
		// every start/end after using date objects and use that one
		// saying plusSeconds()

		switch (this.groupBy[0]) {
		case "week":
		    if (this.endAfter != null)
			endDate = computeEndDate(startDate, this.endAfter[0], howManyEndDate);

		    groupedTestJobs = everyWeek(howManyStartDate, startDate, endDate, packageName);
		    classifier = howManyStartDate + "week";
		    break;
		case "day":
		default:
		    if (this.endAfter != null)
			endDate = computeEndDate(startDate, this.endAfter[0], howManyEndDate);
		    groupedTestJobs = everyDay(howManyStartDate, startDate, endDate, packageName);
		    classifier = howManyStartDate + "day";
		    break;
		}

		for (Entry<DateTime, Set<TestJob>> testjobsPerDay : groupedTestJobs) {
		    logger.debug("Jobs per day " + testjobsPerDay.getKey() + " " + testjobsPerDay.getValue());
		    // TODO Output files using the classifier
		    Writer outputWriter = new FileWriter(
			    "test-jobs_" + packageName + "_" + (testjobsPerDay.getKey().getMillis() / 1000) + ".yml");
		    YamlWriter yamlWriter = new YamlWriter(outputWriter);
		    yamlWriter.write(testjobsPerDay.getValue());
		    yamlWriter.close();
		}
	    }
	    // With the group by option we start at submitAt and go till the end
	    // !

	    return null;
	}
    }

    private long getMaxSubmitAt() throws SQLException {
	try (Connection c = DriverManager.getConnection(connection); Statement stmt = c.createStatement();) {
	    c.setAutoCommit(false);
	    String includeQuery = "SELECT MAX(submit_at) FROM TestJob;";
	    try (ResultSet rs = stmt.executeQuery(includeQuery)) {
		rs.next();
		return rs.getLong(1); // Or 0 ?!
	    }
	}
    }

    private final String QUERY_TEST_JOBS_BETWEEN_DATES = "SELECT * FROM TestJob WHERE submit_at >= %s AND submit_at < %s AND name='%s' ORDER BY submit_at ASC";

    // private final AtomicInteger counter = new AtomicInteger(0); == Test Jobs
    // have uniq ID

    public List<Entry<DateTime, Set<TestJob>>> everyDay(int howMany, DateTime startDate, DateTime endDate,
	    String packageName) throws ClassNotFoundException {
	// Do not cut at the beginning of the day !
	DateTime startSlot = startDate;// .withTimeAtStartOfDay();
	DateTime endDay = endDate;// .withTimeAtStartOfDay();
	//
	DateTime theNextDay = startSlot.plusDays(howMany);
	//

	List<Entry<DateTime, Set<TestJob>>> result = new ArrayList<Entry<DateTime, Set<TestJob>>>();
	while (theNextDay.isBefore(endDay) || theNextDay.minusDays(1).isBefore(endDay)) {

	    String theQuery = String.format(QUERY_TEST_JOBS_BETWEEN_DATES, //
		    startSlot.getMillis() / 1000, //
		    theNextDay.getMillis() / 1000, packageName);

	    result.add(new AbstractMap.SimpleEntry<DateTime, Set<TestJob>>(startSlot, getTestJobsSubmitted(theQuery)));

	    startSlot = theNextDay;
	    theNextDay = startSlot.plusDays(howMany);
	}

	//
	Collections.sort(result, comparator);

	return result;
    }

    // LIST OF TEST JOBS (NAME, VERSION, SUBMIT_AT) --> FILE YAML
    public static void main(String[] args) throws ParseException, ClassNotFoundException, IOException, SQLException {
	TestJobGenerator tjg = new TestJobGenerator();
	tjg.parseArgs(args);
	//
	tjg.generate();
	// TODO Move output to file stdout later, i.e., here

    }
}
