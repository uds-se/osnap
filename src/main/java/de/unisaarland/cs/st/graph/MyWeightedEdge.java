package de.unisaarland.cs.st.graph;

import org.jgrapht.graph.DefaultWeightedEdge;

public class MyWeightedEdge<S, T> extends DefaultWeightedEdge {

    private S source;
    private T destination;

    /**
     * 
     */
    private static final long serialVersionUID = 4099561120081030438L;

    @Override
    public String toString() {
	// return source + " -[" + super.getWeight() + "]-> " + destination;
	return String.format("%s", super.getWeight());
    }

    public int getTime() {
	return (int) super.getWeight();
    }

//    public double getWeight() {
//	return super.getWeight();
//    }

    public S getSource() {
	return this.source;
    }

    public T getDestination() {
	return this.destination;
    }

    public void setSource(S source) {
	this.source = source;

    }

    public void setDestination(T destination) {
	this.destination = destination;

    }

}
