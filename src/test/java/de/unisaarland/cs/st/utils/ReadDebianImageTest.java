package de.unisaarland.cs.st.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Package;

public class ReadDebianImageTest {

	@Test
	public void testWrite() {
		YamlWriter writer;
		try {
			File temp = File.createTempFile("test-image-", ".yml");
			temp.deleteOnExit();
			writer = new YamlWriter(new FileWriter(temp));

			Set<Image> availableImages = new HashSet<Image>();
			Image test = new Image();
			Package p = new Package();
			p.name = "package1";
			p.version = "0.001";
			test.installedPackages.add(p);
			p = new Package();
			p.name = "package2";
			p.version = "0.002";
			test.installedPackages.add(p);

			availableImages.add(test);

			test = new Image();
			p = new Package();
			p.name = "package3";
			p.version = "0.001";
			test.installedPackages.add(p);
			p = new Package();
			p.name = "package4";
			p.version = "0.002";
			test.installedPackages.add(p);
			availableImages.add(test);

			System.out.println("ReadDebianImageTest.testWrite() " + availableImages);

			writer.write((HashSet<Image>) availableImages);

			writer.close();

			System.out.println("ReadDebianImageTest.testWrite() Check " + temp.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Exception", e);
		} finally {
		}
	}

	@Test
	public void testRead() {
		YamlReader reader;
		try {
			reader = new YamlReader(new FileReader("src/main/resources/de.unisaarland.cs.st/evaluation/images/debian-8.yml"));
			Set<Image> availableImages = reader.read(Set.class, Image.class);
			System.out.println("ReadDebianImageTest.testRead() " + availableImages);
		} catch (FileNotFoundException | YamlException e) {
			e.printStackTrace();
			Assert.fail("Exception", e);
		}
	}
}
