package de.unisaarland.cs.st.graph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.Package;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.mappers.OpportunisticSnapshot;

public class OpportunisticSnapshotNetworkFlowModel {

    private Logger logger = Logger.getLogger(getClass());

    public long timeStats[] = new long[] { -1L, -1L, -1L, -1L, -1L, -1L };
    public long nodeStats[] = new long[] { -1L, -1L };
    public long edgeStats[] = new long[] { -1L, -1L };

    // TODO Why those vars are protected ?!
    protected DirectedAcyclicGraph<Object, MyWeightedEdge> networkFlow;
    // DEBUG
    // protected DirectedAcyclicGraph<Object, MyWeightedEdge> installationGraph;

    // public DirectedAcyclicGraph<Object, MyWeightedEdge>
    // getInstallationGraph() {
    // return installationGraph;
    // }

    protected Set<Image> availableImages;
    // TODO Make this configurable instead of EMPTY !
    // public final static Image BASE_IMAGE = new Image();
    protected Image baseImage;
    protected Set<TestJob> testJobs;
    protected CloudModel cloudModel;
    protected Goal goal;

    // Default Values
    public int installationThreshold = 50; // Consider the installation of deps
					   // only if the cumulative cost/tim
					   // is more than this
    public int imageThreshold = 50; // Consider to create a new image node if
				    // the installation cost/time is more than
				    // this
    //
    public long nodesInsertionTime;
    public long arcInsertionTime;
    public long simplificationTime;

    // Index
    protected Set<MyWeightedEdge<SourceNode, Image>> snapshotArcs;
    protected Set<MyWeightedEdge<Image, Image>> installationArcs;
    //
    protected Set<MyWeightedEdge<Image, TestNode>> testArcs;
    protected Set<TestNode> testNodes;
    //
    protected SourceNode sourceNode = SourceNode.get(); // Really ?! Should not
							// be
    // something different that
    // Image
    // ?

    public Object getSourceNode() {
	return sourceNode;
    }

    public Set<TestNode> getTestNodes() {
	return testNodes;
    }

    public Set<TestJob> getTestJobs() {
	return testJobs;
    }

    // OBJECT -> IMAGE
    public Set<MyWeightedEdge<SourceNode, Image>> getSnapshotArcs() {
	return snapshotArcs;
    }

    // IMAGE -> IMAGE
    public Set<MyWeightedEdge<Image, Image>> getInstallationArcs() {
	return installationArcs;
    }

    public Set<Image> getInstallationNodes() {
	// Compute this on the fly
	Set<Image> installationNodes = new HashSet<Image>();
	for (Object o : networkFlow.vertexSet()) {
	    if (o instanceof Image)
		installationNodes.add((Image) o);
	}
	return installationNodes;
    }

    // IMAGE -> TEST
    public Set<MyWeightedEdge<Image, TestNode>> getTestArcs() {
	return testArcs;
    }

    /**
     * Assume that baseImage is in availableImages !
     * 
     * @param testJobs
     * @param availableImages
     * @param baseImage
     * @param cloudModel
     * @param goal
     * @throws CycleFoundException
     */
    public OpportunisticSnapshotNetworkFlowModel(Set<TestJob> testJobs, Set<Image> availableImages, Image baseImage,
	    CloudModel cloudModel, Goal goal) throws CycleFoundException {
	// By default disable the optimizations
	this(testJobs, availableImages, baseImage, cloudModel, goal.installationThreshold, goal.imageThreshold);
	this.goal = goal;
    }

    /**
     * 
     * @param testJobs
     * @param availableImages
     * @throws CycleFoundException
     */
    public OpportunisticSnapshotNetworkFlowModel(Set<TestJob> testJobs, Set<Image> availableImages, Image baseImage,
	    CloudModel cloudModel, int installationThreshold, int imageThreshold) throws CycleFoundException {

	this.testJobs = testJobs;
	this.cloudModel = cloudModel;
	this.availableImages = availableImages;
	this.baseImage = baseImage;
	//

	//

	this.snapshotArcs = new HashSet<MyWeightedEdge<SourceNode, Image>>();
	this.installationArcs = new HashSet<MyWeightedEdge<Image, Image>>();
	this.testArcs = new HashSet<MyWeightedEdge<Image, TestNode>>();
	this.testNodes = new HashSet<TestNode>();

	//
	this.installationThreshold = installationThreshold;
	this.imageThreshold = imageThreshold;

	Set<Package> allDeps = new HashSet<>();
	for (TestJob t : testJobs) {
	    allDeps.addAll(t.getAllPackages());
	}

	logger.info("OpportunisticSnapshot.OpportunisticSnapshot() All deps : " + allDeps.size());

	long start, end;

	// FOR DEBUG. Create the installation graph 2 time !;
	// installationGraph = createTheInstallationDAG();

	networkFlow = createTheInstallationDAG();
	logger.info("InstallationDAG " + networkFlow.vertexSet().size() + ":" + networkFlow.edgeSet().size());
	logger.trace(networkFlow.toString());

	start = System.currentTimeMillis();
	insertSnapshotArcs();
	end = System.currentTimeMillis();
	timeStats[4] = (end - start);

	logger.info("Snapshots Arc + InstallationDAG " + networkFlow.vertexSet().size() + ":"
		+ networkFlow.edgeSet().size());
	logger.trace(networkFlow.toString());

	start = System.currentTimeMillis();
	insertTestNodes();
	end = System.currentTimeMillis();
	timeStats[5] = (end - start);

	logger.info("Snapshots Arc + InstallationDAG + targetNodes " + networkFlow.vertexSet().size() + ":"
		+ networkFlow.edgeSet().size());
	logger.trace(networkFlow.toString());

	// This does not really remove a thing, probably it does not event
	// follow the direction of the graph.
	// Connectivity means any link ! TODO Can be changed
	// finalCleanUp(networkFlow);

	// logger.info("Snapshots Arc + InstallationDAG + targetNodes Final
	// CLEAN UP"
	// + networkFlow.vertexSet().size()
	// + ":"
	// + networkFlow.edgeSet().size());
	// logger.trace(networkFlow.toString());

    }

    private void finalCleanUp(DirectedAcyclicGraph<Object, MyWeightedEdge> theGraph) {

	// This might take a lot of time ?
	ConnectivityInspector<Object, MyWeightedEdge> cInsp = new ConnectivityInspector(theGraph);

	// Compute the connected graph from sourceNode
	List<Set<Object>> connectedSets = cInsp.connectedSets();

	// Note that because we explicitly linked nodes with TEST NODES all the
	// TEST NODES should be reachable from the Installation DAG !
	// TODO Not sure about that...
	for (Set<Object> nodeSet : connectedSets) {
	    // Remove the nodes not connected with the source, they are not
	    // reachable
	    if (!nodeSet.contains(sourceNode)) {
		logger.warn("\n\n **** Node set does not contains source remove all the nodes ! *** \n\n ");
		// Check no test nodes are there !
		theGraph.removeAllVertices(nodeSet);
	    }
	    // TODO: Remove the nodes that do not lead to any test node
	}
    }

    /*
     * TODO: Improvement: create an index on the available images and one for
     * the remaining images, avoid to call the loop 2 times with find all in
     * between !
     */
    protected void insertSnapshotArcs() throws CycleFoundException {

	long timeToSnapshot = cloudModel.getTimeToSnapshot();

	// Add the syntetic sourceNode - not a real image
	// Object theSourceNode = new Object();
	// theSourceNode.name = "SOURCE";
	// theSourceNode.installedPackages.add(SOURCE_PACKAGE);
	networkFlow.addVertex(this.sourceNode);

	// Connect the SOURCE with all the other nodes (this is basically what
	// will happen anyway with this implementation)

	for (Object vertex : networkFlow.vertexSet()) {
	    if (vertex.equals(this.sourceNode)) {
		continue;
	    }

	    if (!(vertex instanceof Image)) {
		continue;
	    }

	    Image targetNode = (Image) vertex;

	    MyWeightedEdge edge = networkFlow.addDagEdge(this.sourceNode, targetNode);
	    // NOTE
	    edge.setSource(this.sourceNode);
	    edge.setDestination(targetNode);

	    // vertex/targetNode is actually one of the availableImages
	    if (findMatchingImage(availableImages, targetNode) != null) {
		logger.debug("Snapshotting arc for an Available Image " + targetNode + " with cost " + 0
			+ " building it will cost " + targetNode.getTotalInstallationTime());
		networkFlow.setEdgeWeight(edge, 0);
	    } else {
		// Compute the actual cost from source with no packages and the
		// snapshot cost/time. By
		// construction packages of baseImage are already included here.
		// Note that this is also the biggest cost one might pay to
		// generate the targetNode !
		long weight = targetNode.getTotalInstallationTime();
		// The final cost, including the cost of creating a snapshot,
		// will be updated later
		logger.debug("Snapshotting arc for Installation Node " + targetNode + " with cost " + weight);
		networkFlow.setEdgeWeight(edge, weight);
	    }

	    this.snapshotArcs.add(edge);
	}

	// Now compute the shortest path from theSource to all the other nodes
	// to assign costs
	BellmanFordShortestPath<Object, MyWeightedEdge> bfsp = new BellmanFordShortestPath<Object, MyWeightedEdge>(
		networkFlow, this.sourceNode);

	for (Object vertex : networkFlow.vertexSet()) {
	    if (vertex.equals(this.sourceNode)) {
		continue;
	    }

	    if (!(vertex instanceof Image)) {
		continue;
	    }

	    // If the snapshot is already there, time of snapshot is zero
	    double weight = 0;

	    if (findMatchingImage(availableImages, (Image) vertex) == null) {

		weight = bfsp.getCost(vertex) + timeToSnapshot;

		if (timeToSnapshot < Double.MAX_VALUE) {
		    MyWeightedEdge edge = networkFlow.getEdge(this.sourceNode, vertex);
		    if (edge != null) {
			logger.trace("Update Snapshotting arc " + edge.getSource() + "-->" + edge.getDestination()
				+ " with weight " + weight);
			networkFlow.setEdgeWeight(edge, weight);
		    }
		} else {
		    logger.info("Snapshotting arc inhibited ! ");
		}
	    } else {
		logger.trace("Skip SNAPSHOTTING ARC FOR AVAILABLE IMAGE");
		continue;
	    }

	}
    }

    /**
     * 
     * Look up an image ONLY by deps
     * 
     * @param theGraph
     * @param theImage
     * @return
     */
    protected Image findMatchingImage(DirectedAcyclicGraph<Object, MyWeightedEdge> theGraph, Image theImage) {
	// Find this element !
	for (Object o : theGraph.vertexSet()) {
	    if (o instanceof Image && ((Image) o).installedPackages.equals(theImage.installedPackages)) {
		logger.trace("findMatchingImage Found matching image: " + ((Image) o));
		return (Image) o;
	    }
	}
	return null;
    }

    /**
     * Given theImage return a ref to an image in the availableImages set
     * 
     * @param images
     * @param theImage
     * @return
     */
    protected Image findMatchingImage(Set<Image> images, Image theImage) {
	// Find this element !
	for (Image i : images) {
	    if (i.installedPackages.equals(theImage.installedPackages)) {
		logger.trace("findMatchingImage Found matching image: " + i);
		return i;
	    }
	}
	return null;
    }

    // TODO This probably could work also by passing as node directly the
    // TestJob objects

    protected void insertTestNodes() throws CycleFoundException {

	for (TestJob testJob : testJobs) {
	    // TODO Probably Test Nodes are not necessary and we can live with
	    // the
	    // Actual TestJob objects
	    TestNode testNode = new TestNode();
	    testNode.testJob = testJob;
	    // testNode.installedPackages.add(TARGET_PACKAGE);
	    // testNode.installedPackages.addAll(testJob.getDependencies());
	    testNode.name = String.format(OpportunisticSnapshot.TEST_ID_TO_TEST_NODE_NAM, testJob.id);
	    networkFlow.addVertex(testNode);

	    //
	    logger.debug("Adding Test Node " + testNode);
	    //
	    // Note Bookeeping
	    this.testNodes.add(testNode);
	    //
	    // Link to all the others
	    //
	    // Complete the diagram by inserting the proper links
	    for (Object vertex : networkFlow.vertexSet()) {
		// We care only about internal Image nodes
		if (vertex instanceof Image) {
		    Image sourceNode = (Image) vertex;

		    // If this installation node is a superset of test node
		    // (packages) add the link
		    if (sourceNode.installedPackages.containsAll(testJob.getAllPackages())) {

			MyWeightedEdge edge = networkFlow.addDagEdge(sourceNode, testNode);
			networkFlow.setEdgeWeight(networkFlow.getEdge(sourceNode, testNode), 0);
			// Bookeping
			edge.setSource(sourceNode);
			edge.setDestination(testNode);
			this.testArcs.add(networkFlow.getEdge(sourceNode, testNode));

			logger.debug("Adding Test arc " + sourceNode + " --> " + testNode);

		    }
		}
	    }
	}
    }

    // http://stackoverflow.com/questions/1670862/obtaining-a-powerset-of-a-set-in-java
    // Also this can be used:
    // http://stackoverflow.com/questions/18466258/finding-power-set-of-a-set
    // private <T> Set<Set<T>> powerSet(Set<T> originalSet) {
    // Set<Set<T>> sets = new HashSet<Set<T>>();
    // if (originalSet.isEmpty()) {
    // sets.add(new HashSet<T>());
    // return sets;
    // }
    // List<T> list = new ArrayList<T>(originalSet);
    // T head = list.get(0);
    // Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
    // for (Set<T> set : powerSet(rest)) {
    // Set<T> newSet = new HashSet<T>();
    // newSet.add(head);
    // newSet.addAll(set);
    // sets.add(newSet);
    // sets.add(set);
    // }
    // return sets;
    // }

    /**
     * Heuristic: treat TB.d as single units and proceeds as follow:
     * <ul>
     * <li>introduce the TB.d state</li>
     * <li>introduce a union state with all the TB.d states already there -
     * split</li>
     * <li>introduce an intersection state with all the TB.d states already -
     * union there</li>
     * </ul>
     * 
     * <strong>Intoduce an additional set of parameters: split-threshold,
     * union-threshold to control the addition of new nodes</strong>
     * 
     * (for the moment union and intersection are power set of TB, so it might
     * still be too much, but definitively less than the original solution
     * 
     * @param testBatches2
     * @return
     * @throws CycleFoundException
     */

    // Consider only pairs of test batches
    /**
     * (n su k) = n! k! (n−k)!,if 0≤k≤n n # tests k = 2
     * 
     * or n^2 ?
     * 
     * @return
     * @throws CycleFoundException
     */
    protected DirectedAcyclicGraph<Object, MyWeightedEdge> createTheInstallationDAG() throws CycleFoundException {

	DirectedAcyclicGraph<Object, MyWeightedEdge> theGraph = new DirectedAcyclicGraph<>(MyWeightedEdge.class);

	/*
	 * NOTE: We must either use the baseImage or EmptyImage, cannot be both.
	 * In other words, we use empty only if declared as baseImage
	 */

	// Insert default based image
	// theGraph.addVertex(BASE_IMAGE);
	logger.debug("Adding Base Image " + baseImage + " with " + baseImage.installedPackages.size()
		+ " packages and cost " + baseImage.getTotalInstallationTime());
	theGraph.addVertex(this.baseImage);

	// Introduce the nodes corresponding to the snapshots, but do not add
	// empty ! If it's baseImage it's already there
	long start, end;
	start = System.currentTimeMillis();
	for (Image snapshot : availableImages) {
	    if (snapshot.equals(Image.getEmptyImage()))
		continue;
	    if (snapshot.equals(this.baseImage))
		continue;

	    logger.debug(
		    "Adding Available Image" + snapshot + " with " + snapshot.installedPackages.size() + " packages");
	    theGraph.addVertex(snapshot);
	}

	end = System.currentTimeMillis();
	timeStats[2] = (end - start);

	start = System.currentTimeMillis();
	// Create the middles nodes, aka Test Nodes.
	// NOTE: test nodes must include the basic installation packages, i.e.,
	// baseImage.installed packages as well. from baseImage one cannot
	// remove packages, since all the packages there are deemed necessary by
	// design.
	Set<TestJob> insertedTestJobs = new HashSet<TestJob>();
	for (TestJob testJob : testJobs) {

	    // Create the minimal image for running the tb, without including
	    // the SUT
	    Image virtualMachine = new Image();
	    // Add all the packages from BaseImage !
	    virtualMachine.installedPackages.addAll(baseImage.installedPackages);
	    // Add the actual packages required for testing
	    // TODO What about updates at baseImage?!
	    for (Package base : baseImage.installedPackages) {
		for (Package actual : testJob.getAllPackages()) {
		    if (base.name.equals(actual.name) && !base.version.equals(actual.version)) {
			System.err.println("\t\t\t Updates on base package " + base + " -> " + actual);
		    }
		}
	    }
	    virtualMachine.installedPackages.addAll(testJob.getAllPackages());
	    //
	    virtualMachine.name = "" + testJob;
	    //
	    logger.debug("Adding Image " + virtualMachine.getId() + " for Test Job " + testJob + " with "
		    + virtualMachine.installedPackages.size() + " packages and total cost "
		    + virtualMachine.getTotalInstallationTime() + " with Base cost "
		    + virtualMachine.getSetupTimeWithImage(baseImage));
	    theGraph.addVertex(virtualMachine);
	}
	end = System.currentTimeMillis();
	timeStats[0] = (end - start);

	// Create a simple combination generator to generate 2-combinations of
	// the initial vector
	// TODO Really this should be done recursively until a fix point such as
	// the {Udeps} and {empty}

	start = System.currentTimeMillis();
	Generator<TestJob> generator = Factory.createSimpleCombinationGenerator(Factory.createVector(testJobs), 2);

	logger.debug("Test jobs: " + testJobs.size());
	List<ICombinatoricsVector<TestJob>> allPairs = generator.generateAllObjects();
	logger.trace("Internal node count: " + allPairs.size());

	long startNode = System.currentTimeMillis();

	for (ICombinatoricsVector<TestJob> testJobSet : allPairs) {

	    TestJob testJob_1 = testJobSet.getValue(0);
	    TestJob testJob_2 = testJobSet.getValue(1);
	    //
	    logger.trace("Processing Pair : " + testJob_1 + ", " + testJob_2);
	    //
	    Image t1 = new Image();
	    t1.installedPackages.addAll(baseImage.installedPackages);
	    t1.installedPackages.addAll(testJob_1.getAllPackages());
	    //
	    Image t2 = new Image();
	    t2.installedPackages.addAll(baseImage.installedPackages);
	    t2.installedPackages.addAll(testJob_2.getAllPackages());
	    //
	    //
	    // Introduce INTERSECTION IMAGES to increase the prob of sharing a
	    // vm between testbatches.
	    // TODO Note that i package update vanno gestiti correttamente, di
	    // fatto non ci possono essere
	    // immagini con lo stesso pacchetto in due versioni allo stesso
	    // momento
	    Image intersectVirtualMachine = new Image();
	    intersectVirtualMachine.name = testJob_1 + " AND " + testJob_2;

	    Set<Package> intersection = new HashSet<Package>();
	    //
	    intersection.addAll(baseImage.installedPackages);
	    //
	    intersection.addAll(testJob_1.getAllPackages());
	    intersection.retainAll(testJob_2.getAllPackages());
	    //
	    intersectVirtualMachine.installedPackages.addAll(intersection);

	    // long delta1 = t1.getTotalInstallationTime() -
	    // intersectVirtualMachine.getTotalInstallationTime();
	    long delta1 = t1.getSetupTimeWithImage(intersectVirtualMachine);
	    // long delta2 = t2.getTotalInstallationTime() -
	    // intersectVirtualMachine.getTotalInstallationTime();
	    long delta2 = t2.getSetupTimeWithImage(intersectVirtualMachine);

	    // Heuristic: the new node must be different enough from the tests
	    // nodes

	    // Add the image as node in the graph

	    logger.trace("intersection delta 1 " + delta1);
	    logger.trace("intersection delta 2 " + delta2);

	    //
	    if (delta1 > imageThreshold && delta2 > imageThreshold) {
		// Check if an image with the same set of packages is already
		// there !
		// TODO This might cost A LOT !!
		// TODO Move to private method
		if (findMatchingImage(theGraph, intersectVirtualMachine) == null) { // This
										    // check
										    // by-value
		    logger.debug("Intersection Image " + intersectVirtualMachine + " already in the graph");
		} else if (theGraph.addVertex(intersectVirtualMachine)) { // This
									  // check
									  // by-ref
		    logger.debug("Adding Intersection Image " + intersectVirtualMachine + " with "
			    + intersectVirtualMachine.installedPackages.size() + " packages");
		} else {
		    logger.debug("Intersection Image " + intersectVirtualMachine + " already in the graph");
		}
	    } else {
		logger.debug("Enforcing imageThreshold. Skip Intersection VM " + intersectVirtualMachine);
	    }

	    // If the images have conflicting packages, like same name different
	    // version, the union cannot be done !
	    if (doConflict(testJob_1.getAllPackages(), testJob_2.getAllPackages())) {
		System.err.println("Conflicting Test Jobs ! Cannot create UNION VM!");
		logger.info("Conflicting Test Jobs ! Cannot create UNION VM!");
		continue;
	    }
	    // Introduce UNION IMAGES to increase the prob of sharing a vm
	    // between testbatches. Pay attention to packages with the same name
	    // in different versions !
	    Image unionVirtualMachine = new Image();
	    //
	    unionVirtualMachine.name = testJob_1 + " OR " + testJob_2;
	    Set<Package> union = new HashSet<Package>();
	    //
	    union.addAll(baseImage.installedPackages);
	    //
	    union.addAll(testJob_1.getAllPackages());
	    union.addAll(testJob_2.getAllPackages());
	    //
	    unionVirtualMachine.installedPackages.addAll(union);
	    //
	    // delta1 = unionVirtualMachine.getTotalInstallationTime() -
	    // t1.getTotalInstallationTime();
	    delta1 = unionVirtualMachine.getSetupTimeWithImage(t1);
	    // delta2 = unionVirtualMachine.getTotalInstallationTime() -
	    // t2.getTotalInstallationTime();
	    delta2 = unionVirtualMachine.getSetupTimeWithImage(t2);

	    logger.trace("union delta 1 " + delta1);
	    logger.trace("union delta 2 " + delta2);

	    // Add the image as node in the graph
	    if (delta1 > imageThreshold && delta2 > imageThreshold) {

		if (theGraph.addVertex(unionVirtualMachine)) {
		    logger.debug("Adding Union Image " + unionVirtualMachine + " with "
			    + unionVirtualMachine.installedPackages.size() + " packages");
		}
	    } else {
		logger.debug("Enforcing imageThreshold. Skip Union VM " + unionVirtualMachine);
	    }
	}

	nodesInsertionTime = System.currentTimeMillis() - startNode;
	logger.info("Done Node additions in " + (nodesInsertionTime / 1000));

	end = System.currentTimeMillis();
	timeStats[1] = (end - start);
	// Complete the diagram by inserting the proper links under the
	// constraints of split-union thresholds. Introduce a link only if the
	// cost is greater than the threshold

	long startArc = System.currentTimeMillis();
	Image[] nodes = theGraph.vertexSet().toArray(new Image[0]);
	for (int source = 0; source < nodes.length; source++) {
	    for (int target = 0; target < nodes.length; target++) {

		if (source == target) // Skip loops
		    continue;

		Image sourceNode = nodes[source];
		Image targetNode = nodes[target];

		// If a.deps is subset of b.deps then a.deps -> b.deps and cost
		// is the difference
		// elseif a.deps is superset of b.deps then b.deps -> a.deps
		// else do not link the two unless there is the possibility to
		// update the package version from source to target
		// This works because we iterate for both source and target, not
		// just once !
		// MyWeightedEdge edge = null;
		// long weight = -1;
		// Image from = null;
		// Image to = null;

		// [1]Consider only the direction of increasing number of
		// packages or updates

		Set<Package> snapshotWithUpdates = getWithUpdates(sourceNode.installedPackages,
			targetNode.installedPackages);

		if (snapshotWithUpdates == null) {
		    logger.warn(targetNode + " cannot be a target for " + sourceNode
			    + ". There was a problem with the updates.");
		    continue;
		}

		//
		if (!targetNode.installedPackages.containsAll(snapshotWithUpdates)) {
		    continue;
		}

		// Try to add the edge, It returns null if the edge is already
		// there

		// Add the edge only if there is no path between from and to
		List<MyWeightedEdge> path = DijkstraShortestPath.findPathBetween(theGraph, sourceNode, targetNode);

		// Not sure anymore :(
		if (path == null) {

		    // Compute the cost
		    // Target node has more deps than source node because of [1]

		    // Replaced by: addInstallationArc
		    // weight = targetNode.getTotalInstallationTime() -
		    // sourceNode.getTotalInstallationTime();
		    // weight = targetNode.getSetupTimeWithImage(sourceNode);

		    // if (weight < installationThreshold) {
		    // logger.debug("Enforcing installationThreshold. Cannot add
		    // edge " + sourceNode + " -> "
		    // + targetNode + " because weight " + weight + " <" +
		    // installationThreshold
		    // + " installationThreshold");
		    // continue;
		    // }
		    //
		    // edge = theGraph.addDagEdge(sourceNode, targetNode);

		    // if (edge != null) { // Avoid duplicates
		    if (addInstallationArc(theGraph, sourceNode, targetNode)) {
			// logger.trace("Adding edge " + sourceNode + "(" +
			// sourceNode.getTotalInstallationTime() + ")"
			// + " -> " + targetNode + " (" +
			// targetNode.getTotalInstallationTime() + ")");
			// Book keeping
			// MyWeightedEdge edge = null;
			// long weight = -1;
			// edge = theGraph.getEdge(sourceNode, targetNode);
			// edge.setSource(sourceNode);
			// edge.setDestination(targetNode);
			// this.installationArcs.add(edge);

			//
			// logger.trace("Added edge " + edge + " with weight " +
			// weight);
			// theGraph.setEdgeWeight(edge, weight);

			logger.trace("Enforce transitive reduction");

			// Apply transitive reduction to remove additional arcs
			// -
			// note that sourceNode is the y, ie, the middle !
			//
			// for (Image from : theGraph.vertexSet()) {
			for (MyWeightedEdge incomingEdge : theGraph.incomingEdgesOf(sourceNode)) {

			    Object from = theGraph.getEdgeSource(incomingEdge);
			    if (from.equals(sourceNode)) {
				continue;
			    }

			    if (from.equals(targetNode)) {
				continue;
			    }

			    if (theGraph.containsEdge(from, sourceNode)
				    && theGraph.containsEdge(sourceNode, targetNode)) {
				logger.trace(String.format("Found (%s->%s) and (%s->%s) thus remove (%s->%s)", from,
					sourceNode, sourceNode, targetNode, from, targetNode));
				theGraph.removeEdge(from, targetNode);
			    }
			}
		    }

		} else {
		    logger.trace(String.format("Skip edge (%s -> %s) because a path exists (%s) ", sourceNode,
			    targetNode, path.toString()));
		    continue;
		}
	    }
	}
	arcInsertionTime = (System.currentTimeMillis() - startArc);
	logger.debug("Done Arcs additions in " + (arcInsertionTime / 1000));

	// TODO FIXME Should this be necessary also now that we remove links one
	// by
	// one ?
	// Remove unecessary arcs by performing a TransitiveReduction

	long startSimplify = System.currentTimeMillis();

	edgeStats[0] = theGraph.edgeSet().size();
	nodeStats[0] = theGraph.vertexSet().size();
	///
	int edgeBefore = theGraph.edgeSet().size();
	logger.debug("Reducing graph to minimum (total edges " + edgeBefore + ")");
	TransitiveReduction.INSTANCE.reduce(theGraph);
	int edgeNow = theGraph.edgeSet().size();
	int gain = (int) (((double) (edgeBefore - edgeNow)) / (double) edgeBefore * 100);

	simplificationTime = (System.currentTimeMillis() - startSimplify);
	edgeStats[1] = theGraph.edgeSet().size();
	nodeStats[1] = theGraph.vertexSet().size();

	logger.debug("done (" + edgeNow + " == " + gain + ") in " + (simplificationTime / 1000));
	end = System.currentTimeMillis();
	timeStats[3] = simplificationTime;
	// TODO Another heuristic would be to use dominant solutions: e.g. if I
	// have
	// already a snapshot X all the paths from SOURCE to X should be removed
	// because for sure taking directly that snapshot is better or at least
	// equivalent

	return theGraph;
    }

    // There is at least one package with same name and differente versio
    boolean doConflict(Set<Package> allPackages, Set<Package> allPackages2) {
	for (Package p : allPackages) {
	    for (Package p2 : allPackages2) {

		// if (p.name == null || p.version == null) {
		// System.err.println("ERROR WITH PACKAGE " + p);
		// }

		if (p.name.equals(p2.name) && p.version.compareTo(p2.version) != 0) {
		    System.err.println(p + " conflicts with " + p2);
		    return true;
		}
	    }
	}

	return false;
    }

    public DirectedAcyclicGraph<Object, MyWeightedEdge> getNetworkFlow() {
	return this.networkFlow;
    }

    // /* protected */static OpportunisticSnapshotNetworkFlowModel
    // createAnotherTestModel() throws CycleFoundException {
    //
    // DataDriver d = DataDriver.createAnotherTestData();
    //
    // return new OpportunisticSnapshotNetworkFlowModel(d.testJobs,
    // d.availableImages, Image.getEmptyImage(),
    // d.cloudModel, d.goal);
    // }
    //
    // /* protected */static OpportunisticSnapshotNetworkFlowModel
    // createTestModel() throws CycleFoundException {
    //
    // DataDriver d = DataDriver.createTestData();
    //
    // return new OpportunisticSnapshotNetworkFlowModel(d.testJobs,
    // d.availableImages, Image.getEmptyImage(),
    // d.cloudModel, d.goal);
    // }

    protected Set<Package> getWithUpdates(Set<Package> oldPackages, Set<Package> newPackages) {
	// Add all the old ones
	Set<Package> withUpdates = new HashSet<Package>(oldPackages);

	// Replace the new ones
	for (Package oldVersion : oldPackages) {
	    for (Package newVersion : newPackages) {
		if (oldVersion.name.equals(newVersion.name) && oldVersion.version.compareTo(newVersion.version) < 0) {
		    //
		    if (!withUpdates.remove(oldVersion) || !withUpdates.add(newVersion)) {
			// How is possible that the node do not include
			// this one ?!
			logger.error("** ERROR !! CANNOT Simulate updating " + oldVersion
				+ " to check for full inclusion. Do not consider the image anymore. Contains ? "
				+ withUpdates.contains(oldVersion) + " " + withUpdates.contains(newVersion));
			return null;
		    }
		}
	    }
	}
	return withUpdates;
    }

    // Return the list of new packages
    protected Set<Package> getUpdates(Set<Package> oldPackages, Set<Package> newPackages) {
	Set<Package> withUpdates = new HashSet<Package>();

	// Replace the new ones
	for (Package oldVersion : oldPackages) {
	    for (Package newVersion : newPackages) {
		if (oldVersion.name.equals(newVersion.name) && oldVersion.version.compareTo(newVersion.version) < 0) {
		    // Somehow in the same image there is the same package
		    // twice, in two versions !
		    //
		    if (!withUpdates.add(newVersion)) {
			System.err.println("** DUPLICATE PACKAGE !! " + oldVersion);
			logger.warn("** DUPLICATE PACKAGE !! " + oldVersion);
			// For the moment just ignore and overwrite !
			// return null;
		    }
		}
	    }
	}
	return withUpdates;
    }

    protected boolean addPackageUpdateArc(DirectedAcyclicGraph<Object, MyWeightedEdge> theGraph, Image sourceNode,
	    Image targetNode) throws CycleFoundException {
	// Look for packages in base that might have been updated in the
	// meanwhile !
	// Simulate the update of source
	Set<Package> updatedSourceNodeInstalledPackages = getWithUpdates(sourceNode.installedPackages,
		targetNode.installedPackages);
	// Check for full inclusion using the updated packages. At this point,
	// the updated packages must be a subset of target !
	if (!targetNode.installedPackages.containsAll(updatedSourceNodeInstalledPackages)) {
	    logger.warn("Even after the updates target node " + targetNode + " is not a super set of " + sourceNode);

	    //
	    // List what is missing in target which instead is in source !
	    Set<Package> t = new HashSet<Package>(targetNode.installedPackages);
	    Set<Package> s = new HashSet<Package>(updatedSourceNodeInstalledPackages);

	    s.removeAll(targetNode.installedPackages);
	    t.removeAll(updatedSourceNodeInstalledPackages);

	    logger.trace("Missing packages S - T " + s.size() + " " + s);
	    logger.trace("Missing packages T - S " + t.size() + " " + t);
	    //

	    return false;
	} else {
	    Image updatedSourceImage = new Image();
	    updatedSourceImage.installedPackages = updatedSourceNodeInstalledPackages;

	    MyWeightedEdge edge = null;
	    // Weight here is the sum of installation and download for updating
	    // packages !
	    long weight = 0;
	    Set<Package> mustUpdate = getUpdates(sourceNode.installedPackages, targetNode.installedPackages);
	    for (Package p : mustUpdate) {
		weight = weight + p.installationTime + p.downloadTime;
	    }

	    //

	    if (weight == 0) {
		Set<Package> target = new HashSet<Package>();
		if (targetNode.getSetupTimeWithImageMillisec(updatedSourceImage) <= 0) {
		    target.addAll(targetNode.installedPackages);
		    target.removeAll(updatedSourceImage.installedPackages);
		    logger.warn("Missing deps: " + target + " gives weight " + weight);
		} else {
		    // Default to 1 if less than a sec. Note that this is
		    weight = 1;
		}
	    }

	    // if (weight < 0) {
	    // logger.warn("negative weight for update egde " + sourceNode + "
	    // --> " + targetNode + " weight is "
	    // + weight);
	    // }

	    // NOTE HERE WE KEEP THE ORIGINAL SOURCE NODE NOT THE UPDATED ONE !!
	    edge = theGraph.addDagEdge(sourceNode, targetNode);

	    if (edge != null) { // Avoid duplicates
		// Book keeping
		edge.setSource(sourceNode);
		edge.setDestination(targetNode);
		theGraph.setEdgeWeight(edge, weight);
		//
		this.installationArcs.add(edge);
		//
		logger.debug("\t Linking " + sourceNode + " -> " + targetNode + " with cost " + weight
			+ " (UPDATE PACAKGES)");

		return true;
	    }

	    return false;

	}

    }

    protected boolean addInstallationArc(DirectedAcyclicGraph<Object, MyWeightedEdge> theGraph, Image sourceNode,
	    Image targetNode) throws CycleFoundException {
	MyWeightedEdge edge = null;
	long weight = -1;

	if (sourceNode.equals(targetNode)) {
	    logger.warn("Try to add installation arc on same image. Loops not allowed !");
	    return false;
	}

	// [1]Consider only the direction of increasing number of
	// packages
	// [2] Consider the possibility of updating package versions !

	// This might happen because we invoke add in both direction
	// irrespsective of the dependencies
	if (targetNode.installedPackages.size() < sourceNode.installedPackages.size()) {
	    logger.debug("Target node " + targetNode + " has less deps than source " + sourceNode);
	    return false;
	}

	// not only it must contains all of them, but some more !
	if (!targetNode.installedPackages.containsAll(sourceNode.installedPackages)) {
	    logger.debug("Target node " + targetNode + " is not a super set of " + sourceNode);
	    // List what is missing in target which instead is in source !
	    Set<Package> t = new HashSet<Package>(targetNode.installedPackages);
	    Set<Package> s = new HashSet<Package>(sourceNode.installedPackages);

	    s.removeAll(targetNode.installedPackages);
	    t.removeAll(sourceNode.installedPackages);

	    logger.trace("Missing packages S - T " + s.size() + " " + s);
	    logger.trace("Missing packages T - S " + t.size() + " " + t);

	    logger.debug("Try to add Package update edge");
	    return addPackageUpdateArc(theGraph, sourceNode, targetNode);

	}
	// Compute the cost
	// Target node has more deps than source node because of
	// [1]
	// This is better otherswise by rounding we occasionally lose a second !
	// weight = targetNode.getTotalInstallationTime() -
	// sourceNode.getTotalInstallationTime();
	weight = targetNode.getSetupTimeWithImage(sourceNode);
	// The only way they are different is that source node is not strictly
	// contained in target node !

	// This might happen for very small packages that have setup time < 500
	// msec. We use long and round up the values to seconds
	if (weight == 0) {
	    Set<Package> target = new HashSet<Package>();
	    //
	    // TODO Check for duplicates otherwise it is fine - Sometimes target
	    // is empty and this is still triggered which somehow should be
	    // avoided using findMatchingImage ?
	    if (targetNode.getSetupTimeWithImageMillisec(sourceNode) <= 0) {
		target.addAll(targetNode.installedPackages);
		target.removeAll(sourceNode.installedPackages);
		if (target.size() > 0) {
		    logger.warn("Missing deps: " + target + " gives weight " + weight);
		} else {
		    logger.warn("Same dependencies but not same image? " + sourceNode + " <--> " + targetNode);
		    return false;
		}
	    }

	    // Iterator<Package> iterator = target.iterator();
	    // while (iterator.hasNext()) {
	    // String depName = iterator.next().name;
	    // for (Package s : sourceNode.installedPackages) {
	    // if (s.name.startsWith(depName))
	    // logger.error("Source : " + s.name + "_" + s.version + " " +
	    // s.downloadTime + " "
	    // + s.installationTime);
	    // }
	    // for (Package t : targetNode.installedPackages) {
	    // if (t.name.startsWith(depName))
	    // logger.error("Target : " + t.name + "_" + t.version + " " +
	    // t.downloadTime + " "
	    // + t.installationTime);
	    // }
	    // }
	}

	if (weight < 0) {
	    logger.warn("negative weight for installation egde " + sourceNode + " --> " + targetNode + " weight is "
		    + weight);
	    return false;
	}

	if (weight < installationThreshold) {
	    logger.warn("Enforcing installationThreshold. Cannot add edge " + sourceNode + " -> " + targetNode
		    + " because weight " + weight + " <" + installationThreshold + " installationThreshold");
	    return false;
	}

	edge = theGraph.addDagEdge(sourceNode, targetNode);

	if (edge != null) { // Avoid duplicates
	    // Book keeping
	    edge.setSource(sourceNode);
	    edge.setDestination(targetNode);
	    theGraph.setEdgeWeight(edge, weight);
	    //
	    this.installationArcs.add(edge);
	    //
	    logger.debug("\t Linking " + sourceNode + " -> " + targetNode + " with cost " + weight);

	    return true;
	}

	return false;
    }

}
