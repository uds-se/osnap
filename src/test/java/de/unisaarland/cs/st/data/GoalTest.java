package de.unisaarland.cs.st.data;

import java.io.FileNotFoundException;
import java.io.FileReader;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

public class GoalTest {

    @Test
    public void equalsAndHasCode() throws YamlException, FileNotFoundException {
	String goalFile = "src/test/resources/goal.yml";
	YamlReader goalReader = new YamlReader(new FileReader(goalFile));
	Goal goal1 = goalReader.read(Goal.class);
	//
	goalReader = new YamlReader(new FileReader(goalFile));
	Goal goal2 = goalReader.read(Goal.class);
	Assert.assertEquals(goal1, goal1);
	//
	Assert.assertEquals(goal1, goal2);
	//
	Assert.assertEquals(goal1.hashCode(), goal2.hashCode());
    }
}
