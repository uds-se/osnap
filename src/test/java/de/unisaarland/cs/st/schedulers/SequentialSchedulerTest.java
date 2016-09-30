package de.unisaarland.cs.st.schedulers;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Schedule;

@Test
public class SequentialSchedulerTest extends AbstractSchedulerTest {

    @BeforeMethod
    public void setSchedulerAsBeforeMethod() {
	scheduler = new SequentialScheduler();
    }

    @Override
    void makeAssertionOnSchedule(Schedule schedule) {
	System.out.println("SequentialSchedulerTest.makeAssertionOnSchedule() " + schedule);
	Assert.assertNotNull(schedule);
	Assert.assertEquals(schedule.jobsDistribution.keySet().size(), 1);
	Instance instance = schedule.jobsDistribution.keySet().iterator().next();
	Assert.assertTrue(instance.isReserved());
    }

}
