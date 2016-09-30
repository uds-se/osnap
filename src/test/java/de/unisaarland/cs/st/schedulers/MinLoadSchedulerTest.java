package de.unisaarland.cs.st.schedulers;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Schedule;

@Test
public class MinLoadSchedulerTest extends AbstractSchedulerTest {

    @BeforeMethod
    public void setSchedulerAsBeforeMethod() {
	scheduler = new MinLoadScheduler();

    }

    @Override
    void makeAssertionOnSchedule(Schedule schedule) {
	Assert.assertNotNull(schedule);
	System.out.println("MinLoadSchedulerTest.makeAssertionOnSchedule() " + schedule);

	// limit on the amount specified in goal !
	Assert.assertEquals(schedule.jobsDistribution.keySet().size(), goal.maxOnDemandInstances);
	for (Instance instance : schedule.jobsDistribution.keySet()) {
	    Assert.assertTrue(instance.isReserved());
	}

    }

}
