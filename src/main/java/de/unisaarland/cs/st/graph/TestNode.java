package de.unisaarland.cs.st.graph;

import java.io.Serializable;

import de.unisaarland.cs.st.data.TestJob;

public class TestNode implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5557920937328064535L;
	public String name;
	public TestJob testJob;

	@Override
	public String toString() {
		return "Test Node "
				+ ((testJob != null) ? String.format("%s", testJob.id) : (name != null) ? name : super.toString());
//				+ "\n" + testJob.getAllDependencies();
	}

	public Object getLabel() {
		return "TestNode_" + testJob.id;
	}
}
