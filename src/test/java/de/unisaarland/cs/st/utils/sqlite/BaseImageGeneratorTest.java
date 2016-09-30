package de.unisaarland.cs.st.utils.sqlite;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.ParseException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlReader;

import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Package;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.util.sqlite.BaseImageGenerator;

public class BaseImageGeneratorTest {

    @Test
    public void mainTest() {
	try {
	    BaseImageGenerator.main(new String[] { "--db-name",
		    "/Users/gambi/ICST2017/icst2017-osnap/evaluation/icst2017.db", "--submit-at", "1443685462" });
	} catch (ClassNotFoundException | ParseException | IOException e) {
	    e.printStackTrace();
	    Assert.fail("Exception", e);
	}
    }

    @Test
    public void mainTestWithOutputFile() {
	try {
	    File temp = File.createTempFile("temp-file-name", ".tmp");
	    temp.deleteOnExit();
	    //
	    String submitAt = "1443685462";
	    //
	    BaseImageGenerator
		    .main(new String[] { "--db-name", "/Users/gambi/ICST2017/icst2017-osnap/evaluation/icst2017.db",
			    "--submit-at", submitAt, "--output-file", temp.getAbsolutePath() });

	    // Assert file exists and not empty
	    Assert.assertTrue(temp.exists());
	    Assert.assertTrue(temp.length() > 0);
	    // Ideally assert that this can be parsed just fine !
	    YamlReader testJobsReader = new YamlReader(new FileReader(temp));
	    // Set
	    Image baseImage = testJobsReader.read(Image.class);

	    Assert.assertEquals(baseImage.name, submitAt);
	    Assert.assertEquals(baseImage.parentImage, Image.getEmptyImage());
	    Assert.assertTrue(baseImage.installedPackages != null);
	    Assert.assertTrue(baseImage.installedPackages.size() > 0);

	} catch (ClassNotFoundException | ParseException | IOException e) {
	    e.printStackTrace();
	    Assert.fail("Exception", e);
	}
    }

    @Test
    public void mainTestBaseImageAsAvailableImages() {
	try {
	    File temp = File.createTempFile("temp-file-name", ".tmp");
	    temp.deleteOnExit();
	    //
	    String submitAt = "1443685462";
	    //
	    BaseImageGenerator
		    .main(new String[] { "--db-name", "/Users/gambi/ICST2017/icst2017-osnap/evaluation/icst2017.db",
			    "--submit-at", submitAt, "--output-file", temp.getAbsolutePath(), "--output-as-set" });

	    // Assert file exists and not empty
	    Assert.assertTrue(temp.exists());
	    Assert.assertTrue(temp.length() > 0);
	    // Can we parse the single image as set ?
	    // Ideally assert that this can be parsed just fine !
	    YamlReader availableImageReader = new YamlReader(new FileReader(temp));
	    // TODO Not sure need Set or HashSet
	    Set<Image> availableImages = availableImageReader.read(HashSet.class, Image.class);
	    // Contains also empty
	    Assert.assertTrue(availableImages.size() == 2); 

	    Image baseImage = availableImages.iterator().next();
	    Assert.assertEquals(baseImage.name, submitAt);
	    Assert.assertEquals(baseImage.parentImage, Image.getEmptyImage());
	    Assert.assertTrue(baseImage.installedPackages != null);
	    Assert.assertTrue(baseImage.installedPackages.size() > 0);

	} catch (ClassNotFoundException | ParseException | IOException e) {
	    e.printStackTrace();
	    Assert.fail("Exception", e);
	}
    }
}
