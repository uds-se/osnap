package de.unisaarland.cs.st.data;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

public class ImageTest {

    String availableImageFile = "src/test/resources/available-images.yml";

    @Test
    public void equalsAndHashCodeTestForAvailableImages() throws YamlException, FileNotFoundException {

	YamlReader availableImageReader = new YamlReader(new FileReader(availableImageFile));
	Set<Image> availableImages1 = availableImageReader.read(Set.class, Image.class);
	//
	availableImageReader = new YamlReader(new FileReader(availableImageFile));
	Set<Image> availableImages2 = availableImageReader.read(Set.class, Image.class);

	// Image baseImage = Image.getEmptyImage();
	Assert.assertEquals(availableImages1, availableImages1);
	//
	Assert.assertEquals(availableImages1, availableImages2);
	//
	Assert.assertEquals(availableImages1.hashCode(), availableImages2.hashCode());
    }

    @Test
    public void equalsAndHashCodeTest() throws YamlException, FileNotFoundException {
	YamlReader availableImageReader = new YamlReader(new FileReader(availableImageFile));
	Set<Image> availableImages1 = availableImageReader.read(Set.class, Image.class);
	availableImages1.remove(Image.getEmptyImage());
	// Assume at least 1 image other than empty
	Image baseImage1 = availableImages1.iterator().next();
	//
	availableImageReader = new YamlReader(new FileReader(availableImageFile));
	Set<Image> availableImages2 = availableImageReader.read(Set.class, Image.class);
	availableImages2.remove(Image.getEmptyImage());
	Image baseImage2 = availableImages2.iterator().next();
	// Image baseImage = Image.getEmptyImage();

	Assert.assertEquals(baseImage1, baseImage1);
	//
	Assert.assertEquals(baseImage1, baseImage2);
	//
	Assert.assertEquals(baseImage1.hashCode(), baseImage2.hashCode());
    }

}
