package de.unisaarland.cs.st.graph;

import java.io.Serializable;

public class SourceNode implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4783462050117615459L;
	private final String name = "SOURCE";
	private static SourceNode INSTANCE;

	public synchronized static SourceNode get() {
		if (INSTANCE == null)
			INSTANCE = new SourceNode();

		return INSTANCE;
	}

	@Override
	public String toString() {
		return name;
	}
}
