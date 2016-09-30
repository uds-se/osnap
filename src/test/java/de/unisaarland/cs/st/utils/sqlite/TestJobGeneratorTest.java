package de.unisaarland.cs.st.utils.sqlite;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.ParseException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlReader;

import de.unisaarland.cs.st.data.Package;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.util.sqlite.TestJobGenerator;

public class TestJobGeneratorTest {

    @Test
    public void mainTest() {
	try {
	    TestJobGenerator
		    .main(new String[] { "--db-name", "/Users/gambi/ICST2017/icst2017-osnap/evaluation/icst2017.db",
			    "--sample-size", "1", "--submit-at", "1443685462" });
	} catch (ClassNotFoundException | ParseException | IOException | SQLException e) {
	    e.printStackTrace();
	    Assert.fail("Exception", e);
	}
    }

    @Test
    public void mainTestOnlySUT() {
	try {
	    TestJobGenerator tjg = new TestJobGenerator();
	    tjg.parseArgs(new String[] { "--db-name", "/Users/gambi/ICST2017/icst2017-osnap/evaluation/icst2017.db",
		    "--sample-size", "1", "--submit-at", "1443685462", "--output-only-sut" });
	    Set<TestJob> result = (Set<TestJob>) tjg.generate();

	} catch (ClassNotFoundException | ParseException | IOException | SQLException e) {
	    e.printStackTrace();
	    Assert.fail("Exception", e);
	}
    }

    // 1444440708
    String pName1 = "libperl6-caller-perl";
    String pName2 = "libtest-file-sharedir-perl";
    String pName3 = "libcatalyst-plugin-subrequest-perl";

    @Test
    public void mainTestWithIncludeAndSmallSize() {
	try {
	    TestJobGenerator tjg = new TestJobGenerator();
	    tjg.parseArgs(new String[] { "--db-name", "/Users/gambi/ICST2017/icst2017-osnap/evaluation/icst2017.db",
		    "--sample-size", "1", "--submit-at", "1444440708", "--include", pName1, pName2, pName3 });
	    Set<TestJob> result = (Set<TestJob>) tjg.generate();
	    // Assert that pName3 is there
	    for (TestJob t : result) {
		if (t.sut.name.equals(pName3))
		    return;
	    }
	    Assert.fail("Include package is missing");

	} catch (ClassNotFoundException | ParseException | IOException | SQLException e) {
	    e.printStackTrace();
	    Assert.fail("Exception", e);
	}
    }

    @Test
    public void mainTestWithIncludeAndSameSize() {
	try {
	    TestJobGenerator tjg = new TestJobGenerator();
	    tjg.parseArgs(new String[] { "--db-name", "/Users/gambi/ICST2017/icst2017-osnap/evaluation/icst2017.db",
		    "--sample-size", "3", "--submit-at", "1444440708", "--include", pName1, pName2, pName3 });
	    Set<TestJob> result = (Set<TestJob>) tjg.generate();
	    Set<String> expectedNames = new HashSet<String>();
	    expectedNames.add(pName1);
	    expectedNames.add(pName2);
	    expectedNames.add(pName3);

	    Set<String> actualNames = new HashSet<String>();
	    // Assert that pName3 is there- We know for sure that the names are
	    // there !
	    for (TestJob t : result) {
		actualNames.add(t.sut.name);
	    }
	    Assert.assertEquals(actualNames, expectedNames);

	} catch (ClassNotFoundException | ParseException | IOException | SQLException e) {
	    e.printStackTrace();
	    Assert.fail("Exception", e);
	}
    }

    @Test
    public void mainTestWithIncludeAndBugSize() {
	try {
	    TestJobGenerator tjg = new TestJobGenerator();
	    tjg.parseArgs(new String[] { "--db-name", "/Users/gambi/ICST2017/icst2017-osnap/evaluation/icst2017.db",
		    "--sample-size", "5", "--submit-at", "1444440708", "--include", pName1, pName2, pName3 });
	    Set<TestJob> result = (Set<TestJob>) tjg.generate();
	    Assert.assertEquals(5, result.size());

	    Set<String> expectedNames = new HashSet<String>();
	    expectedNames.add(pName1);
	    expectedNames.add(pName2);
	    expectedNames.add(pName3);

	    Set<String> actualNames = new HashSet<String>();
	    // Assert that pName3 is there- We know for sure that the names are
	    // there !
	    for (TestJob t : result) {
		actualNames.add(t.sut.name);
	    }
	    Assert.assertTrue(actualNames.containsAll(expectedNames));

	} catch (ClassNotFoundException | ParseException | IOException | SQLException e) {
	    e.printStackTrace();
	    Assert.fail("Exception", e);
	}
    }

    @Test
    public void getFullDataForPackage() throws ClassNotFoundException {
	String dbName = "/Users/gambi/ICST2017/icst2017-osnap/evaluation/icst2017.db";
	String name = "r-bioc-limma";
	String version = "3.24.15+dfsg-1";
	String submitAt = "1443685462";

	TestJob testJob = new TestJob();
	Package sut = new Package();
	sut.name = name;
	sut.version = version;
	testJob.sut = sut;
	testJob.submissionTime = Long.parseLong(submitAt);

	int expectedValue = 300; // Manually extracted from the db

	TestJobGenerator tjg = new TestJobGenerator();
	tjg.setDbName(dbName);

	TestJob t = tjg.updateTestJobDependencies(testJob);
	Assert.assertEquals(t.sut.dependencies.size(), expectedValue);

    }

    @Test
    public void mainTestWithOutputFile() {
	try {
	    File temp = File.createTempFile("temp-file-name", ".tmp");
	    temp.deleteOnExit();
	    //
	    int sampleSize = 5;
	    TestJobGenerator.main(new String[] { "--db-name",
		    "/Users/gambi/ICST2017/icst2017-osnap/evaluation/icst2017.db", "--sample-size", "" + sampleSize,
		    "--submit-at", "1443685462", "--output-file", temp.getAbsolutePath() });

	    // Assert file exists and not empty
	    Assert.assertTrue(temp.exists());
	    Assert.assertTrue(temp.length() > 0);
	    // Ideally assert that this can be parsed just fine !
	    YamlReader testJobsReader = new YamlReader(new FileReader(temp));
	    // Set
	    Set<TestJob> testJobs = testJobsReader.read(Set.class, TestJob.class);

	    Assert.assertEquals(testJobs.size(), sampleSize);

	} catch (ClassNotFoundException | ParseException | IOException | SQLException e) {
	    e.printStackTrace();
	    Assert.fail("Exception", e);
	}
    }

    @Test
    public void testCannotAddPackages() {
	try {
	    TestJobGenerator tjg = new TestJobGenerator();
	    tjg.parseArgs(new String[] { "--db-name", "/Users/gambi/ICST2017/icst2017-osnap/evaluation/icst2017.db",
		    "--sample-size", "10", "--submit-at", "1447295693", "--output-only-sut", "--include",
		    "kdecoration r-cran-hwriter r-cran-dosefinding kdeclarative libcatalyst-authentication-store-htpasswd-perl r-cran-reshape2 r-cran-matrixstats libcatalyst-plugin-static-simple-perl r-cran-epi kde-gtk-config" });
	    Set<TestJob> result = (Set<TestJob>) tjg.generate();

	} catch (ClassNotFoundException | ParseException | IOException | SQLException e) {
	    e.printStackTrace();
	    Assert.fail("Exception", e);
	}
    }

}
