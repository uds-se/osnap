package de.unisaarland.cs.st.util;

import java.util.Set;

import org.jgrapht.graph.SimpleDirectedGraph;

/*
 * Inspired by TransitiveClousure of JGraphT and by
 * http://stackoverflow.com/questions/1690953/transitive-reduction-algorithm-pseudocode 
 */
@Deprecated
public class TransitiveReduction {

	/**
	 * Singleton instance.
	 */
	public static final TransitiveReduction INSTANCE = new TransitiveReduction();

	/**
	 * Private Constructor.
	 */
	private TransitiveReduction() {
	}

	/**
	 * Computes the transitive reduction of the given graph.
	 * 
	 * foreach x in graph.vertices
	 * 
	 * foreach y in graph.vertices if( x != y )
	 * 
	 * foreach z in graph.vertices if( y != z ) delete edge xz if edges xy AND
	 * yz exist
	 *
	 * @param graph
	 *            - Graph to compute transitive reduction for.
	 */
	public <V, E> void reduceSimpleDirectedGraph(SimpleDirectedGraph<V, E> graph) {
		Set<V> vertexSet = graph.vertexSet();

		// Basic implementation, for sure it can be optimized
		for (V x : vertexSet) {
			for (V y : vertexSet) {
				if (x.equals(y)) {
					continue;
				}
				for (V z : vertexSet) {
					if (y.equals(z)) {
						continue;
					}

					if (x.equals(z)) {
						continue;
					}
					// (x,z) != (x,y) && (x,z) != (y,z)

					// Avoid to go on if the edge was already removed
					if (!graph.containsEdge(x, z)) {
						continue;
					}

					if (graph.containsEdge(x, y) && graph.containsEdge(y, z)) {
						// delete edge xz if edges xy AND yz exist
						graph.removeEdge(x, z);
						// System.out
						// .println(String
						// .format("Found (%s->%s) and (%s->%s) thus remove
						// (%s->%s)",
						// x, y, y, z, x, z));
					}

				}
			}
		}
	}
}
// End TransitiveReduction.java
