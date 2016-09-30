package de.unisaarland.cs.st.plot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import de.unisaarland.cs.st.data.Schedule;
import de.unisaarland.cs.st.util.SchedulePlotter;

public class PlotterTestFromFile {

	private Set<Schedule> schedules = new HashSet<Schedule>();

	@BeforeTest
	public void loadSchedules() {
		File folder = new File("src/test/resources/de.unisaarland.cs.st.plot");
		File[] listOfFiles = folder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("recursive-all-in") && name.endsWith(".schedule.sav");
			}
		});

		for (File scheduleFile : listOfFiles) {
			try {
				// YamlReader reader = new YamlReader(new
				// FileReader(scheduleFile));
				// schedules.add(reader.read(Schedule.class));
				System.out.println("Load " + scheduleFile);
				ObjectInputStream restore = new ObjectInputStream(new FileInputStream(scheduleFile));
				schedules.add((Schedule) restore.readObject());
				restore.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//
	}

	@Test
	public void plotScheduleTest() {
		Schedule schedule = schedules.iterator().next();
		File epsFile = null;
		try {
			epsFile = SchedulePlotter.plotSchedule(schedule);
			System.out.println( schedule );
			System.out.println("SchedulePlotterTest.plotSequentialScheduleTest() " + epsFile);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();
		}
	}

}
