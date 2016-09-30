package de.unisaarland.cs.st.evaluation.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import com.esotericsoftware.yamlbeans.YamlWriter;

import de.unisaarland.cs.st.data.DataDriver;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.TestJob;

public class YAMLTestDataGenerator {

	public static void main(String[] args) throws IOException {
		DataDriver driver = DataDriver.createTestData();
		String baseFolder = "src/test/resources/de.unisaarland.cs.st/evaluation/data";

		String availableImages = "available-images.yml";
		String testJobs = "test-jobs.yml";
		String cloudModel = "cloud-model.yml";
		String goal = "goal.yml";

		//
		File availableImagesFile = new File(baseFolder + "/" + availableImages);
		File testJobsFile = new File(baseFolder + "/" + testJobs);
		File cloudModelFile = new File(baseFolder + "/" + cloudModel);
		File goalFile = new File(baseFolder + "/" + goal);

		//
		YamlWriter availableImagesWriter = new YamlWriter(new FileWriter(availableImagesFile));
		// writer.getConfig().setPropertyElementType(Contact.class,
		// "phoneNumbers", Phone.class);
		availableImagesWriter.write((HashSet<Image>) driver.availableImages);
		availableImagesWriter.close();

		YamlWriter testJobsWriter = new YamlWriter(new FileWriter(testJobsFile));
		// writer.getConfig().setPropertyElementType(Contact.class,
		// "phoneNumbers", Phone.class);
		testJobsWriter.write((HashSet<TestJob>) driver.testJobs);
		testJobsWriter.close();

		YamlWriter cloudModelWriter = new YamlWriter(new FileWriter(cloudModelFile));
		// writer.getConfig().setPropertyElementType(Contact.class,
		// "phoneNumbers", Phone.class);
		cloudModelWriter.write(driver.cloudModel);
		cloudModelWriter.close();

		YamlWriter goalWriter = new YamlWriter(new FileWriter(goalFile));
		// writer.getConfig().setPropertyElementType(Contact.class,
		// "phoneNumbers", Phone.class);
		goalWriter.write(driver.goal);
		goalWriter.close();

	}
}
