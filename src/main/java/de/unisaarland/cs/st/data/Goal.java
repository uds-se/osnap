package de.unisaarland.cs.st.data;

public class Goal {

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	long temp;
	temp = Double.doubleToLongBits(alpha);
	result = prime * result + (int) (temp ^ (temp >>> 32));
	temp = Double.doubleToLongBits(beta);
	result = prime * result + (int) (temp ^ (temp >>> 32));
	result = prime * result + imageThreshold;
	result = prime * result + installationThreshold;
	result = prime * result + (int) (maxCost ^ (maxCost >>> 32));
	result = prime * result + maxOnDemandInstances;
	result = prime * result + (int) (maxTime ^ (maxTime >>> 32));
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (!(obj instanceof Goal))
	    return false;
	Goal other = (Goal) obj;
	if (Double.doubleToLongBits(alpha) != Double.doubleToLongBits(other.alpha))
	    return false;
	if (Double.doubleToLongBits(beta) != Double.doubleToLongBits(other.beta))
	    return false;
	if (imageThreshold != other.imageThreshold)
	    return false;
	if (installationThreshold != other.installationThreshold)
	    return false;
	if (maxCost != other.maxCost)
	    return false;
	if (maxOnDemandInstances != other.maxOnDemandInstances)
	    return false;
	if (maxTime != other.maxTime)
	    return false;
	return true;
    }

    private static Goal MIN_TIME;
    private static Goal MIN_COST;

    public synchronized static Goal getMinTimeGoal() {
	if (MIN_TIME == null) {
	    MIN_TIME = new Goal(1, 0);
	}
	return MIN_TIME;
    }

    public synchronized static Goal getMinCostGoal() {
	if (MIN_COST == null) {
	    MIN_COST = new Goal(0, 1);
	}
	return MIN_COST;
    }

    // Default for YAML
    public Goal() {
	this(1, 0);
    }

    public Goal(double alpha, double beta) {
	this.alpha = alpha;
	this.beta = beta;
    }

    // Variable NAMING !
    public final static String EXECUTION_TIME = "total_time";
    public final static String EXECUTION_COST = "total_cost";

    // public String objective = EXECUTION_TIME;
    public long maxTime = -1;
    public long maxCost = -1;
    //
    public int maxOnDemandInstances = -1;
    //
    private double alpha;
    private double beta;
    public int imageThreshold;
    public int installationThreshold;

    public void setAlpha(double alpha) {
	this.alpha = alpha;
    }

    public void setBeta(double beta) {
	this.beta = beta;
    }

    public double getAlpha() {
	return alpha;
    }

    public double getBeta() {
	return beta;
    }

    @Override
    public String toString() {
	return "[objective=" + alpha + " * " + EXECUTION_TIME + " + " + beta + " * " + EXECUTION_COST + " "
		+ ((maxTime > 0) ? ", maxTime=" + maxTime : "") + ((maxTime > 0) ? ", maxCost=" + maxCost : "") + " "
		+ "maxOnDemandInstances=" + maxOnDemandInstances +

		"]";
    }

    public double computeObjective(long totalTime, long totalCost) {

	return totalTime * alpha + totalCost * beta;
    }

}
