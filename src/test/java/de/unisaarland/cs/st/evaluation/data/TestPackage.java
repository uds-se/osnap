package de.unisaarland.cs.st.evaluation.data;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlReader;

import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.TestJob;

public class TestPackage {

	@Test
	public void readTestJob() throws IOException {
		YamlReader reader = new YamlReader(new FileReader(
				"/Users/gambi/Documents/Saarland/TestingPlusCloud/ASE2016/code/src/main/resources/de.unisaarland.cs.st/evaluation/test-jobs/per-threeHours-kdeFamily/20160420_030000.yml"));
		Set<TestJob> testJobs = reader.read(HashSet.class, TestJob.class);

		for (TestJob testJob : testJobs) {
			System.out.println("TestPackage.readTestJob() " + testJob.sut + " " + testJob.submissionTime + " "
					+ testJob.testDuration + " " + testJob.getDownloadTimeWithImage(Image.getEmptyImage()) + " "
					+ testJob.getInstallationTimeWithImage(Image.getEmptyImage()));

		}
	}

	@Test
	public void readTestJobByPackageName() throws IOException {

		File packageDir = new File(
				"/Users/gambi/Documents/Saarland/TestingPlusCloud/ASE2016/code/src/main/resources/de.unisaarland.cs.st/evaluation/test-jobs/per-threeHours-kdeFamily");

		String[] files = packageDir.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".yml");
			}
		});

		for (String file : files) {
			YamlReader reader = new YamlReader(new FileReader(packageDir.getAbsolutePath() + "/" + file));
			Set<TestJob> testJobs = reader.read(HashSet.class, TestJob.class);

			for (TestJob testJob : testJobs) {
				System.out.println("TestPackage.readTestJob() " + testJob.sut + " " + testJob.submissionTime + " "
						+ testJob.testDuration + " " + testJob.getDownloadTimeWithImage(Image.getEmptyImage()) + " "
						+ testJob.getInstallationTimeWithImage(Image.getEmptyImage()));

			}
			reader.close();
		}
	}
}
