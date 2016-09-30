package de.unisaarland.cs.st.evaluation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Schedule;

public class Result implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 3840810128791566041L;

    public Collection<Result> filterByPlanner() {
	List<Result> result = new ArrayList<Result>();

	for (String planner : planners) {
	    Result r = new Result();
	    r.planners = new ArrayList<String>(1);
	    r.planners.add(planner);
	    r.schedules = new HashMap<String, Schedule>();
	    r.schedules.put(planner, this.schedules.get(planner));
	    //
	    result.add(r);
	}
	return result;
    }

    /**
     * 
     */
    // private final EvaluationDriver evaluationDriver;

    /**
     * @param evaluationDriver
     */
    // Result(EvaluationDriver evaluationDriver) {
    // this.evaluationDriver = evaluationDriver;
    // }

    private int getMaxColSize() {
	int max = 0;
	for (String planner : planners) {
	    max = Math.max(max, planner.length());
	}
	return max;
    }

    List<String> planners = new ArrayList<String>();
    Map<String, Schedule> schedules = new HashMap<String, Schedule>();
    // Set of images available at the beginning
    Set<Image> availableImages = new HashSet<Image>();

    public List<String> getPlannerNames() {
	return planners;
    }

    public Map<String, Schedule> getSchedules() {
	return schedules;
    }

    public Set<Image> getAvailableImages() {
	return availableImages;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((planners == null) ? 0 : planners.hashCode());
	result = prime * result + ((schedules == null) ? 0 : schedules.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (!(obj instanceof Result))
	    return false;
	Result other = (Result) obj;
	if (planners == null) {
	    if (other.planners != null)
		return false;
	} else if (!planners.equals(other.planners))
	    return false;
	if (schedules == null) {
	    if (other.schedules != null)
		return false;
	} else if (!schedules.equals(other.schedules))
	    return false;
	return true;
    }

    public void toFile(String outputFile) {
	try (FileOutputStream fout = new FileOutputStream(outputFile);
		ObjectOutputStream oos = new ObjectOutputStream(fout)) {
	    oos.writeObject(this);
	} catch (IOException e) {
	    System.err.println("Cannot store result to file " + outputFile);
	    e.printStackTrace();
	}

    }

    // This will return a different object, so .equals() will not work !
    public static Result fromFile(String inputFile) throws FileNotFoundException, IOException, ClassNotFoundException {
	try (FileInputStream fin = new FileInputStream(inputFile); ObjectInputStream ois = new ObjectInputStream(fin)) {
	    return (Result) ois.readObject();
	}
    }

    // @Override
    public String prettyPrint() {
	int maxColSize = getMaxColSize();

	if (maxColSize < 10)
	    maxColSize = 10;

	String format = "%-" + maxColSize + "s %" + maxColSize + "s %" + maxColSize + "s %" + maxColSize + "s %"
		+ maxColSize + "s\n";

	String header = String.format(format, "Name", // "Goal-Alpha",
		// "Goal-Beta",
		"Objective", "Time", "Cost", "Planning-Time");

	StringBuffer sb = new StringBuffer();
	sb.append(header);
	for (String planner : planners) {
	    Schedule schedule = schedules.get(planner);
	    sb.append(String.format(format, planner, //
		    // this.goal.getAlpha(), this.goal.getBeta(), //
		    String.format("%.3f", schedule.objective), //
		    schedule.getFinalTime(), schedule.getFinalCost(), schedule.totalComputationTime));
	}
	return sb.toString();
    }

    public void mergeWith(Result result) {
	// System.err.println("Mergin " + this.prettyPrint());
	// System.err.println(" with " + result.prettyPrint());
	// Merge planners name
	Set<String> uniqueNames = new HashSet<String>();
	uniqueNames.addAll(planners);
	uniqueNames.addAll(result.planners);
	// Update local structures
	planners.clear();
	planners.addAll(uniqueNames);

	for (String planner : planners) {
	    // System.err.println("Mergin Schedule for " + planner);

	    // Note that empty schedule is then passed around by refernce and
	    // all the schedule points to that !
	    Schedule fromHere = (schedules.containsKey(planner)) ? schedules.get(planner) : Schedule.newEmptySchedule();

	    Schedule fromResult = (result.schedules.containsKey(planner)) ? result.schedules.get(planner)
		    : Schedule.newEmptySchedule();

	    // System.err.println(" fromHere " + fromHere.hashCode() +
	    // fromHere.getFinalTime());
	    // System.err.println(" fromResult " + fromResult.hashCode() +
	    // fromResult.getFinalTime());

	    fromHere.mergeWith(fromResult);

	    // System.err.println("Merged " + fromHere.getFinalTime());

	    schedules.put(planner, fromHere);
	}

    }
}