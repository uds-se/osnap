package de.unisaarland.cs.st.data;

public class CloudModel {

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + (int) (but ^ (but >>> 32));
	result = prime * result + costOfOnDemandInstancePerBUT;
	result = prime * result + costOfReservedInstancePerBUT;
	result = prime * result + nReservedInstances;
	result = prime * result + (int) (timeToSnapshot ^ (timeToSnapshot >>> 32));
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (!(obj instanceof CloudModel))
	    return false;
	CloudModel other = (CloudModel) obj;
	if (but != other.but)
	    return false;
	if (costOfOnDemandInstancePerBUT != other.costOfOnDemandInstancePerBUT)
	    return false;
	if (costOfReservedInstancePerBUT != other.costOfReservedInstancePerBUT)
	    return false;
	if (nReservedInstances != other.nReservedInstances)
	    return false;
	if (timeToSnapshot != other.timeToSnapshot)
	    return false;
	return true;
    }

    // Definition of BUT in seconds
    public long but;

    // Cost of compute
    private int costOfReservedInstancePerBUT;
    private int costOfReservedInstance;
    private int costOfOnDemandInstancePerBUT;

    // Fixed time to create an image from a running instance
    private long timeToSnapshot;

    //
    public int nReservedInstances;

    // Additional Constraints - Optional
    // private long maxTime;
    // private long maxCost;

    // Is this ever used ?
    @Deprecated
    public CloudModel(long but, int costOfReservedInstancePerBUT, int costOfOnDemandInstancePerBUT,
	    int nReservedInstances, long maxTime, long maxCost, long timeToSnapshot) {
	super();
	this.but = but;
	this.costOfReservedInstancePerBUT = costOfReservedInstancePerBUT;
	this.costOfOnDemandInstancePerBUT = costOfOnDemandInstancePerBUT;
	this.nReservedInstances = nReservedInstances;
	// this.maxTime = maxTime;
	// this.maxCost = maxCost;
	this.timeToSnapshot = timeToSnapshot;
    }

    // Needed by Yaml parsing/writing. Make it protected ?
    public CloudModel() {
    }

    public CloudModel(long but, int costOfReservedInstancePerBUT, int costOfOnDemandInstancePerBUT,
	    int nReservedInstances) {
	this(but, costOfReservedInstancePerBUT, costOfOnDemandInstancePerBUT, nReservedInstances, -1L, -1L, 0L);
    }

    public long getBUT() {
	return but;
    }

    // Enable computations using the yearly to hour price (usually very small)
    public int getCostOfReservedInstancePerBUT() {
	return costOfReservedInstancePerBUT;
    }

    // Enable computation using the fixed cost (over the year if you like,
    // otherwise fixed per schedule)
    public int getCostOfReservedInstance() {
	return costOfReservedInstance;
    }

    public void setCostOfReservedInstance(int costOfReservedInstance) {
	this.costOfReservedInstance = costOfReservedInstance;
    }

    public int getCostOfOnDemandInstancePerBUT() {
	return costOfOnDemandInstancePerBUT;
    }

    public int getAvailableReservedInstances() {
	return nReservedInstances;
    }

    public void setBut(long but) {
	this.but = but;
    }

    public void setCostOfOnDemandInstancePerBUT(int costOfOnDemandInstancePerBUT) {
	this.costOfOnDemandInstancePerBUT = costOfOnDemandInstancePerBUT;
    }

    public void setCostOfReservedInstancePerBUT(int costOfReservedInstancePerBUT) {
	this.costOfReservedInstancePerBUT = costOfReservedInstancePerBUT;
    }

    public void setTimeToSnapshot(long timeToSnapshot) {
	this.timeToSnapshot = timeToSnapshot;
    }

    public long getTimeToSnapshot() {
	return timeToSnapshot;
    }

    @Override
    public String toString() {
	return "[but=" + but + ", costOfReservedInstancePerBUT=" + costOfReservedInstancePerBUT
		+ ", costOfOnDemandInstancePerBUT=" + costOfOnDemandInstancePerBUT + ", timeToSnapshot="
		+ timeToSnapshot + ", nReservedInstances=" + nReservedInstances + "]";
    }

}
