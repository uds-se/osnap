package de.unisaarland.cs.st.schedulers;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Schedule;

@Test
public class RandomSchedulerTest extends AbstractSchedulerTest {

    @BeforeMethod
    public void setSchedulerAsBeforeMethod() {
	scheduler = new RandomScheduler();

    }

    @Override
    void makeAssertionOnSchedule(Schedule schedule) {
	System.out.println("RandomSchedulerTest.makeAssertionOnSchedule() " + schedule);
	Assert.assertNotNull(schedule);
	Assert.assertEquals(schedule.jobsDistribution.keySet().size(), goal.maxOnDemandInstances);
	for (Instance instance : schedule.jobsDistribution.keySet()) {
	    Assert.assertTrue(instance.isReserved());
	}

    }

}
