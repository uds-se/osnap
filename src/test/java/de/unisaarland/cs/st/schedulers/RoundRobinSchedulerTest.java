package de.unisaarland.cs.st.schedulers;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Schedule;

@Test
public class RoundRobinSchedulerTest extends AbstractSchedulerTest {

    @BeforeMethod
    public void setSchedulerAsBeforeMethod() {
	scheduler = new RoundRobinScheduler();

    }

    @Override
    void makeAssertionOnSchedule(Schedule schedule) {
	// NOTE: Cannot use job.id to check round robing since they might not be
	// passed in that order to scheduler
	System.out.println("RoundRobinSchedulerTest.makeAssertionOnSchedule() " + schedule);
	Assert.assertNotNull(schedule);
	Assert.assertEquals(schedule.jobsDistribution.keySet().size(), goal.maxOnDemandInstances);
	for (Instance instance : schedule.jobsDistribution.keySet()) {
	    Assert.assertTrue(instance.isReserved());
	}

    }

}
