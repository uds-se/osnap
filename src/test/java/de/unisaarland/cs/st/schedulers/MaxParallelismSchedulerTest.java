package de.unisaarland.cs.st.schedulers;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.unisaarland.cs.st.data.Instance;
import de.unisaarland.cs.st.data.Schedule;

@Test
public class MaxParallelismSchedulerTest extends AbstractSchedulerTest {

    @BeforeMethod
    public void setSchedulerAsBeforeMethod() {
	scheduler = new MaxParallelismScheduler();
    }

    @Override
    void makeAssertionOnSchedule(Schedule schedule) {
	Assert.assertNotNull(schedule);
	Assert.assertEquals(schedule.jobsDistribution.keySet().size(), testJobs.size());
	for (Instance instance : schedule.jobsDistribution.keySet()) {
	    Assert.assertFalse(instance.isReserved());
	}
    }
}
