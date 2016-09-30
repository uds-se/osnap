package de.unisaarland.cs.st.data;

import java.io.FileNotFoundException;
import java.io.FileReader;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

public class CloudModelTest {
    @Test
    public void equalsAndHasCode() throws YamlException, FileNotFoundException {
	String cloudModelFile = "src/test/resources/cloud-model.yml";
	YamlReader cloudModelReader = new YamlReader(new FileReader(cloudModelFile));
	CloudModel cloudModel1 = cloudModelReader.read(CloudModel.class);
	//
	cloudModelReader = new YamlReader(new FileReader(cloudModelFile));
	CloudModel cloudModel2 = cloudModelReader.read(CloudModel.class);

	Assert.assertEquals(cloudModel1, cloudModel1);
	//
	Assert.assertEquals(cloudModel1, cloudModel2);
	//
	Assert.assertEquals(cloudModel1.hashCode(), cloudModel2.hashCode());
    }
}
