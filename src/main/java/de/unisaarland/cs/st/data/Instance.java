package de.unisaarland.cs.st.data;

import java.io.Serializable;

public class Instance implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8876411347411960775L;

    private int id;

    private boolean reserved;

    public double utilization;

    public Instance(int id, boolean reserved) {
	super();
	this.id = id;
	this.reserved = reserved;
    }

    public boolean isReserved() {
	return reserved;
    }

    public int getId() {
	return id;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + id;
	result = prime * result + (reserved ? 1231 : 1237);
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Instance other = (Instance) obj;
	if (id != other.id)
	    return false;
	if (reserved != other.reserved)
	    return false;
	return true;
    }

}
