package de.unisaarland.cs.st.data;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;

// A repository contains a list of packages 
public class Repository {

	public DirectedAcyclicGraph<Package, DefaultEdge> packageDependencies;

	public Repository() {
		packageDependencies = new DirectedAcyclicGraph<Package, DefaultEdge>(
				DefaultEdge.class);

	}

	// TODO Probably an immutable collection would be better
	public Set<Package> getPackages() {
		return packageDependencies.vertexSet();
	}

	public static boolean conflict(Package p, Package q) {
		return p.conflicts.contains(q) || q.conflicts.contains(p);
	}

	// TODO Check if Connected graph is what we really want !
	public Set<Package> getPackageDepencencies(Package sut) {
		ConnectivityInspector<Package, DefaultEdge> ci = new ConnectivityInspector<Package, DefaultEdge>(
				this.packageDependencies);

		Set<Package> deps = new HashSet<Package>(ci.connectedSetOf(sut));
		deps.remove(sut);
		return deps;

	}

	public Package getPackage(String name) {
		for (Package p : this.getPackages()) {
			if (p.name.equals(name)) {
				return p;
			}
		}
		return null;
	}
}
