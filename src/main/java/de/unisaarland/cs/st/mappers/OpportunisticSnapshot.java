package de.unisaarland.cs.st.mappers;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;

import de.unisaarland.cs.st.cplex.MySolverFactoryCPLEX;
import de.unisaarland.cs.st.data.CloudModel;
import de.unisaarland.cs.st.data.Goal;
import de.unisaarland.cs.st.data.Image;
import de.unisaarland.cs.st.data.TestJob;
import de.unisaarland.cs.st.graph.MyWeightedEdge;
import de.unisaarland.cs.st.graph.OpportunisticSnapshotNetworkFlowModel;
import de.unisaarland.cs.st.graph.SourceNode;
import de.unisaarland.cs.st.graph.TestNode;
import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.VarType;

/**
 * Original, non-optimized version of the approach
 * 
 * @author gambi
 *
 */
public class OpportunisticSnapshot implements ITestJobToVMMapper {

    private static Logger logger = Logger.getLogger(OpportunisticSnapshot.class);

    public static String TEST_ID_TO_TEST_NODE_NAM = "%4s";

    private final static int BIG_M = 300000; // Big M method - Large constant

    private static final String FLUX_i_j = "f_%s_%s";
    private static final String SNAPSHOT_i = "s_%s";

    // NOTE: Do no create new nodes if the potential improvement is low
    public int installationThreshold = 50;// 10;
    public int imageThreshold = 50;// 10;

    private Set<TestJob> testJobs;
    private Set<Image> availableImages;
    private Image baseImage;
    private CloudModel cloudModel;
    private Goal goal;

    // This is the optimization problem derived from the model
    private Problem problem;

    // Timing Data
    public long problemCreationTime;
    public long problemSolutionTime;

    // The flow model
    private OpportunisticSnapshotNetworkFlowModel theModel;

    // public OpportunisticSnapshot(Set<TestJob> testJobs, Set<Image>
    // availableImages, CloudModel cloudModel, Goal goal) {
    // this.testJobs = testJobs;
    // this.availableImages = availableImages;
    // this.cloudModel = cloudModel;
    // this.goal = goal;
    // // Create the ILP Problem from scratch
    // createTheProblem();
    // }

    // Would be enough to keep the ILP problem instead ?
    // public OpportunisticSnapshot(OpportunisticSnapshotNetworkFlowModel
    // networkFlowModel) {
    // this.theModel = networkFlowModel;
    // Create the ILP Problem from scratch
    // createTheProblem();
    // }

    //
    private Problem createProblemFromNetworkFlowModel(OpportunisticSnapshotNetworkFlowModel model) {

	Problem problem = new Problem();

	// From the provided flow model extracts problem variables, data and
	// constraints.

	// Total flow to distribute - This is the same as this.testJobs
	int numberOfTestsToExecute = model.getTestJobs().size();

	// Problem Formulation:
	// Objective function
	Linear totalTime = new Linear();

	// Define SNAPSHOTTING Variables, ( 0, 1 )
	// use snapshot/image unique ID to create the problem !
	for (MyWeightedEdge<SourceNode, Image> arc : model.getSnapshotArcs()) {
	    String variable = String.format(SNAPSHOT_i, arc.getDestination().getId());
	    //
	    problem.setVarType(variable, VarType.BOOL);
	    problem.setVarLowerBound(variable, 0);
	    problem.setVarUpperBound(variable, 1);
	    //
	    totalTime.add(arc.getTime(), String.format(SNAPSHOT_i, arc.getDestination().getId()));
	}

	for (MyWeightedEdge<SourceNode, Image> arc : model.getSnapshotArcs()) {
	    String flowVariable = String.format(FLUX_i_j, SourceNode.get(), arc.getDestination().getId());
	    problem.setVarType(flowVariable, VarType.INT);
	    //
	    problem.setVarLowerBound(flowVariable, 0);
	    problem.setVarUpperBound(flowVariable, numberOfTestsToExecute);

	}
	for (MyWeightedEdge<Image, Image> arc : model.getInstallationArcs()) {
	    String flowVariable = String.format(FLUX_i_j, arc.getSource().getId(), arc.getDestination().getId());
	    problem.setVarType(flowVariable, VarType.INT);
	    //
	    problem.setVarLowerBound(flowVariable, 0);
	    problem.setVarUpperBound(flowVariable, numberOfTestsToExecute);
	    //
	    totalTime.add(arc.getTime(),
		    String.format(FLUX_i_j, arc.getSource().getId(), arc.getDestination().getId()));

	}

	// Introduce the Testing FLOW variables
	for (MyWeightedEdge<Image, TestNode> arc : model.getTestArcs()) {
	    String flowVariable = String.format(FLUX_i_j, arc.getSource().getId(), arc.getDestination().getLabel());
	    problem.setVarType(flowVariable, VarType.INT);
	    problem.setVarLowerBound(flowVariable, 0);
	    problem.setVarUpperBound(flowVariable, numberOfTestsToExecute);

	}

	problem.setObjective(totalTime, OptType.MIN);

	// Introduce Flow constraints
	Linear balanceFlowForSource = new Linear();

	// System.out.println("ProblemFormulation.main() BALANCE FLOW in
	// Source");
	for (MyWeightedEdge<SourceNode, Image> arc : model.getSnapshotArcs()) {
	    balanceFlowForSource.add(1, String.format(FLUX_i_j, SourceNode.get(), arc.getDestination().getId()));
	}
	problem.add(balanceFlowForSource, "=", numberOfTestsToExecute);

	// For each instance node repeat the same
	for (TestNode testNode : model.getTestNodes()) {
	    Linear balanceFlowForTarget = new Linear();
	    // System.out.println("ProblemFormulation.main() BALANCE Flow in T_"
	    // + testNode);

	    // Sum all the arcs entering in the test node
	    for (MyWeightedEdge<Image, TestNode> arc : model.getTestArcs()) {
		if (arc.getDestination().equals(testNode)) {

		    // System.out
		    // .println("ProblemFormulation.main() Introduce a Flow
		    // constraint for arc ("
		    // + arc.getSource().getLabel()
		    //
		    // + ", instance"
		    // + arc.getDestination().getLabel()
		    // + ")");

		    balanceFlowForTarget.add(1,
			    String.format(FLUX_i_j, arc.getSource().getId(), arc.getDestination().getLabel()));
		}
	    }

	    // by design there is only one flow for each test to execute !
	    // The sum of all the arcs towards the same target must be one !
	    problem.add(balanceFlowForTarget, "=", 1);
	}

	for (Object node : model.getNetworkFlow().vertexSet()) {

	    Linear balanceFlowForInstallationNodes = new Linear();
	    if (model.getTestNodes().contains(node) || model.getSourceNode().equals(node)) {
		continue;
	    }

	    for (MyWeightedEdge<?, ?> arc : model.getNetworkFlow().incomingEdgesOf(node)) {
		logger.trace("Introduce an Entering Flow constraint for arc (" + arc.getSource() + ", "
			+ arc.getDestination() + ")");

		if (model.getSnapshotArcs().contains(arc)) {
		    balanceFlowForInstallationNodes.add(1,
			    String.format(FLUX_i_j, SourceNode.get(), ((Image) arc.getDestination()).getId()));
		} else if (model.getInstallationArcs().contains(arc)) {
		    balanceFlowForInstallationNodes.add(1, String.format(FLUX_i_j, ((Image) arc.getSource()).getId(),
			    ((Image) arc.getDestination()).getId()));
		} else if (model.getTestArcs().contains(arc)) {
		    balanceFlowForInstallationNodes.add(1, String.format(FLUX_i_j, ((Image) arc.getSource()).getId(),
			    ((TestNode) arc.getDestination()).getLabel()));
		} else {
		    throw new RuntimeException("Invalid type of arc for " + arc);
		}

	    }

	    for (MyWeightedEdge<?, ?> arc : model.getNetworkFlow().outgoingEdgesOf(node)) {
		logger.trace("Introduce an Exiting Flow constraint for arc (" + arc.getSource() + ", "
			+ arc.getDestination() + ")");
		if (model.getSnapshotArcs().contains(arc)) {
		    balanceFlowForInstallationNodes.add(-1,
			    String.format(FLUX_i_j, SourceNode.get(), ((Image) arc.getDestination()).getId()));
		} else if (model.getInstallationArcs().contains(arc)) {
		    balanceFlowForInstallationNodes.add(-1, String.format(FLUX_i_j, ((Image) arc.getSource()).getId(),
			    ((Image) arc.getDestination()).getId()));
		} else if (model.getTestArcs().contains(arc)) {
		    balanceFlowForInstallationNodes.add(-1, String.format(FLUX_i_j, ((Image) arc.getSource()).getId(),
			    ((TestNode) arc.getDestination()).getLabel()));
		} else {
		    throw new RuntimeException("Invalid type of arc for " + arc);
		}

	    }

	    problem.add(balanceFlowForInstallationNodes, "=", 0);
	}

	// Snapshot constraints: - The Big M Approach
	// B*s - f(e) >= 0 or instead f(e) - B(1 - s) <= 0
	// where B is a very large number
	// FIXME is this really right ?

	for (MyWeightedEdge<SourceNode, Image> arc : model.getSnapshotArcs()) {
	    // System.out
	    // .println("ProblemFormulation.main() Introduce a Snapshot take or
	    // leave constraint");

	    // WHy only positive ?
	    // if (arc.getTime() > 0) {
	    Linear snapshotTakeOrLeave = new Linear();

	    snapshotTakeOrLeave.add(1, String.format(FLUX_i_j, SourceNode.get(), arc.getDestination().getId()));
	    snapshotTakeOrLeave.add(-BIG_M, String.format(SNAPSHOT_i, arc.getDestination().getId()));

	    problem.add(snapshotTakeOrLeave, "<=", 0);
	    // }

	}

	// Boundaries defined at variable creation time
	// // Apparently this must be enforced to avoid strange behaviors
	// MIN/MAX
	// // flow
	//
	// for (Object node : model.getNetworkFlow().vertexSet()) {
	// for (MyWeightedEdge arc :
	// model.getNetworkFlow().outgoingEdgesOf(node)) {
	//
	// String variable = null;
	// if (model.getSnapshotArcs().contains(arc)) {
	// variable = String.format(FLUX_i_j, SourceNode.get(), ((Image)
	// arc.getDestination()).getLabel());
	//
	// } else if (model.getInstallationArcs().contains(arc)) {
	// variable = String.format(FLUX_i_j, ((Image)
	// arc.getSource()).getLabel(),
	// ((Image) arc.getDestination()).getLabel());
	// } else if (model.getTestArcs().contains(arc)) {
	// variable = String.format(FLUX_i_j, ((Image)
	// arc.getSource()).getLabel(),
	// ((TestNode) arc.getDestination()).getLabel());
	//
	// } else {
	// throw new RuntimeException("Invalid type of arc for " + arc);
	// }
	//
	// problem.setVarLowerBound(variable, 0);
	// problem.setVarUpperBound(variable, numberOfTestsToExecute);
	//
	// }
	// }

	logger.trace("The problem: \n" + problem);
	return problem;

    }

    // TODO Note that this prints the wrong information
    // it must print if snapshots must be created -> that is before were not
    // there and then they are there
    // it must print which image to run test -> backwards from flow path to test
    // node and take first snapshot that exists or must be created
    private static void printSummaryResult(Result result, //
	    OpportunisticSnapshotNetworkFlowModel networkFlowModel, //
	    Set<Image> availableImages, //
	    Map<TestJob, Image> mappings) throws CycleFoundException {

	StringBuffer summary = new StringBuffer();
	summary.append("Summary of Opportunistic Snapshotting:").append("\n");
	summary.append("\t Min Total Execution Cost :" + result.getObjective()).append("\n");

	StringBuffer snapshotSummary = new StringBuffer();
	snapshotSummary.append("\t Snapshots :");

	boolean create = false;
	for (MyWeightedEdge<?, ?> arc : networkFlowModel.getNetworkFlow()
		.outgoingEdgesOf(networkFlowModel.getSourceNode())) {

	    String idSource, idTarget;

	    if (networkFlowModel.getSnapshotArcs().contains(arc)) {
		idSource = String.format("%s", SourceNode.get());
		idTarget = ((Image) arc.getDestination()).getLabel();

	    } else if (networkFlowModel.getInstallationArcs().contains(arc)) {
		idSource = ((Image) arc.getSource()).getLabel();
		idTarget = ((Image) arc.getDestination()).getLabel();

	    } else if (networkFlowModel.getTestArcs().contains(arc)) {
		idSource = ((Image) arc.getSource()).getLabel();
		idTarget = String.format("%s", ((TestNode) arc.getDestination()).getLabel());
	    } else {
		throw new RuntimeException("Invalid type of arc for " + arc);
	    }

	    if (result.get(String.format(FLUX_i_j, idSource, idTarget)).intValue() > 0) {

		if (arc.getDestination() instanceof Image && !availableImages.contains(arc.getDestination())) {
		    Image snapshot = (Image) arc.getDestination();
		    snapshotSummary.append("Create Snapshot :" + snapshot.getLabel() + " from " + snapshot.parentImage
			    + ". Contain: " + snapshot.installedPackages).append("\n");
		    create = true;
		}
	    }
	}

	if (create) {
	    summary.append(snapshotSummary);
	}

	// Print association TestBatch and Image
	summary.append(String.format("\t %-10s %-10s", "TestBatch", "Snapshot")).append("\n");
	for (Entry<TestJob, Image> mapping : mappings.entrySet()) {
	    summary.append(String.format("\t %-10s %-10s", mapping.getKey().id, mapping.getValue().getLabel()))
		    .append("\n");
	}

	logger.debug(summary.toString());
    }

    private static String printSummaryResult(Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> solution)
	    throws CycleFoundException {

	StringBuffer summary = new StringBuffer();
	summary.append("-------------------------------").append("\n").append("Summary of Opportunistic Snapshotting:")
		.append("\n").append("-------------------------------").append("\n");
	StringBuffer testDeploymentSummary = new StringBuffer();
	if (!solution.getKey().isEmpty()) {
	    testDeploymentSummary.append(String.format("%-20s %-20s", "Test Job", "Image")).append("\n");
	    for (Entry<TestJob, Image> testDeployment : solution.getKey().entrySet()) {
		testDeploymentSummary.append(
			String.format("%-20s %-20s", testDeployment.getKey().id, testDeployment.getValue().getLabel()))
			.append("\n");
	    }
	}

	StringBuffer snapshotSummary = new StringBuffer();
	if (!solution.getValue().isEmpty()) {
	    snapshotSummary.append(String.format("%-20s %-20s", "Snapshots", "Parent Image")).append("\n");
	    for (Entry<Image, Image> newSnapshot : solution.getValue()) {
		snapshotSummary.append(String.format("%-20s %-20s", newSnapshot.getValue().getLabel(),
			newSnapshot.getKey().getLabel())).append("\n");
	    }
	}

	return summary.append(testDeploymentSummary).append(snapshotSummary).toString();
    }

    private int countDeps() {
	int deps = 0;
	for (TestJob t : testJobs) {
	    deps = deps + t.getAllPackages().size();
	}
	return deps;
    }

    private void createTheProblem() {

	long startProblemCreation = System.currentTimeMillis();
	try {
	    if (this.theModel != null) {
		logger.info("Use the provided network model use that");
	    } else {
		logger.debug("Build the model with (conservative): " + countDeps());
		// With must either use Empty or a BaseImage. Empty is only for
		// theoretical studies, as in reality a Base Image is always
		// required
		this.theModel = new OpportunisticSnapshotNetworkFlowModel(testJobs, availableImages, baseImage,
			cloudModel, goal);
	    }

	    this.problem = createProblemFromNetworkFlowModel(theModel);
	    problemCreationTime = (System.currentTimeMillis() - startProblemCreation);
	    logger.info("Problem created in " + (problemCreationTime / 1000));

	} catch (CycleFoundException e) {
	    throw new RuntimeException("Infeasible problem setting: cyclic dependency !", e);
	}

    }

    // FIXME This is wrong: in some cases there is not flow across base image
    // despite that will be used to
    //
    // This is a list (better a set) of parent=>snapshot images!
    private List<Entry<Image, Image>> computeSnapshots(Result result) throws CycleFoundException {
	List<Entry<Image, Image>> mapping = new ArrayList<Entry<Image, Image>>();

	// Create an overlay graph to compute the best snapshotting strategy
	DirectedAcyclicGraph<Image, MyWeightedEdge> overlay = new DirectedAcyclicGraph<Image, MyWeightedEdge>(
		MyWeightedEdge.class);

	// Add to this graph all the Image node of the computed one
	for (Image imageNode : theModel.getInstallationNodes()) {
	    overlay.addVertex(imageNode);
	}
	// Add all the installation arcs of the original graph
	for (MyWeightedEdge<Image, Image> installationEdge : theModel.getInstallationArcs()) {
	    MyWeightedEdge<Image, Image> edge = overlay.addDagEdge(installationEdge.getSource(),
		    installationEdge.getDestination());
	    // TODO Use the Graph call instead ! overlay.getEdgeSource(edge)
	    edge.setSource(installationEdge.getSource());
	    edge.setDestination(installationEdge.getDestination());
	    //
	    overlay.setEdgeWeight(edge, installationEdge.getTime());
	}

	for (MyWeightedEdge<SourceNode, Image> snapshotEdge : theModel.getSnapshotArcs()) { // theModel.getNetworkFlow().outgoingEdgesOf(theModel.getSourceNode()))
											    // {
	    if (result.get(String.format(FLUX_i_j, SourceNode.get(), snapshotEdge.getDestination().getId()))
		    .intValue() > 0) {

		if (!availableImages.contains(snapshotEdge.getDestination())) {
		    // This identifies the need to create a new snapshot
		    Image newSnapshot = snapshotEdge.getDestination();
		    Image originForSnapshot = null;

		    // TODO: Find out the best image to start from using
		    // Dijstrack. This must consider ONLY existing nodes and not
		    // the snapshotting arcs (> 0) otherwise it will always take
		    // the snapshot arc that was computed !
		    // List<MyWeightedEdge> path =
		    // DijkstraShortestPath.findPathBetween(theModel.getNetworkFlow(),
		    // theModel.getSourceNode(), newSnapshot);
		    List<MyWeightedEdge> path = DijkstraShortestPath.findPathBetween(overlay, baseImage, newSnapshot);

		    if (logger.isTraceEnabled()) {
			logger.trace("BEST STRATEGY to create snapshot: " + path);
			for (MyWeightedEdge edge : path) {
			    logger.trace(edge.getSource() + " -> " + edge.getDestination());
			}
		    }

		    // Now we go backwards up to the point we found an available
		    // image or the empty one (SOURCE)
		    Collections.reverse(path);
		    Iterator<MyWeightedEdge> iterator = path.iterator();
		    while (iterator.hasNext()) {
			Object node = iterator.next().getSource();
			if (node instanceof SourceNode)
			    break;
			Image internalNode = (Image) node;
			if (availableImages.contains(internalNode)) {
			    // We found the source image
			    originForSnapshot = internalNode;
			    break;
			}
		    }
		    // else here we should have it or source
		    if (originForSnapshot == null && availableImages.contains(baseImage)) {
			logger.warn("NULL PARENT IMAGE FOR " + newSnapshot + " FORCE BASE IMAGE ");
			originForSnapshot = this.baseImage; // theModel.BASE_IMAGE;
		    } else if (originForSnapshot == null && !availableImages.contains(baseImage)) {
			logger.warn("NULL PARENT IMAGE FOR " + newSnapshot + " FORCE EMPTy IMAGE ");
			originForSnapshot = Image.getEmptyImage();
		    }
		    mapping.add(new AbstractMap.SimpleEntry(originForSnapshot, newSnapshot));
		    //

		}
	    }
	}

	return mapping;

    }

    // TODO We use an euristic to associate images and test job
    private Map<TestJob, Image> computeTestDeployment(Result result) throws CycleFoundException {
	Map<TestJob, Image> mappings = new HashMap<TestJob, Image>();

	DirectedAcyclicGraph<Object, MyWeightedEdge> flowGraph = new DirectedAcyclicGraph(MyWeightedEdge.class);
	for (Object node : theModel.getNetworkFlow().vertexSet()) {
	    flowGraph.addVertex(node);
	}
	// Brute force
	for (Object start : theModel.getNetworkFlow().vertexSet()) {

	    String idStart = (start instanceof Image) ? ((Image) start).getId()
		    : (start instanceof TestNode) ? String.format("%s", ((TestNode) start).getLabel())
			    : String.format("%s", SourceNode.get());

	    for (Object target : theModel.getNetworkFlow().vertexSet()) {
		{
		    String idTarget = (target instanceof Image) ? ((Image) target).getId()
			    : (target instanceof TestNode) ? String.format("%s", ((TestNode) target).getLabel())
				    : String.format("%s", SourceNode.get());
		    Number flow = result.get(String.format(FLUX_i_j, idStart, idTarget));
		    if (flow != null && flow.intValue() > 0) {
			// Introduce an arc with weight only if there is a flow
			// variable
			// different that zero
			MyWeightedEdge e = flowGraph.addDagEdge(start, target);
			logger.trace("Adding edge of the flow graph " + flow.intValue());
			flowGraph.setEdgeWeight(e, flow.intValue());
		    } else {
			continue;
		    }
		}
	    }
	}

	logger.trace("OpportunisticSnapshot.printSummaryResult() " + flowGraph);

	// Compute the path from SOURCE to the first TEST
	// Remove 1 unit of flow from each flow weight
	for (TestNode testNode : theModel.getTestNodes()) {
	    // Prints the shortest path from vertex i to vertex c.
	    List<MyWeightedEdge> path = DijkstraShortestPath.findPathBetween(flowGraph, theModel.getSourceNode(),
		    testNode);
	    //
	    // The first link is the image association
	    logger.debug("Test " + testNode + " uses " + ((Image) flowGraph.getEdgeTarget(path.get(0))).prettyPrint()
		    + "\n");

	    mappings.put(testNode.testJob, (Image) flowGraph.getEdgeTarget(path.get(0)));

	    // Update the weight - remove edge if weight is zero
	    for (MyWeightedEdge e : path) {
		if (flowGraph.getEdgeWeight(e) > 1) {
		    flowGraph.setEdgeWeight(e, flowGraph.getEdgeWeight(e) - 1);
		} else {
		    logger.trace("Simplify graph remove edge with 0 weight: " + e.getSource() + "-" + e.getDestination()
			    + " " + e);
		    flowGraph.removeEdge(e);
		}

	    }
	}

	return mappings;
    }

    // FIXME TODO Implement me !
    public Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> solve() {

	SolverFactory factory = null;
	Solver solver = null;
	Result result = null;
	//
	long startTime = -1;
	long endTime = -1;

	try {

	    factory = new MySolverFactoryCPLEX();
	    if (logger.getLevel() != null && !logger.getLevel().isGreaterOrEqual(Level.DEBUG)) {
		factory.setParameter(Solver.VERBOSE, 100);
	    } else {
		factory.setParameter(Solver.VERBOSE, 0);
	    }
	    factory.setParameter(Solver.TIMEOUT, 600); // 10 mins ?
	    //
	    solver = factory.get();
	    //
	    startTime = System.currentTimeMillis();
	    //
	    result = solver.solve(problem);
	    //
	    endTime = System.currentTimeMillis();

	    problemSolutionTime = endTime - startTime;
	    logger.info("Optimization Problem solved in " + ((endTime - startTime) / 1000));
	} catch (java.lang.UnsatisfiedLinkError exception) {
	    exception.printStackTrace();
	    throw exception;
	}
	if (result != null) {

	    logger.trace(result);

	    try {
		Map<TestJob, Image> deploymentOfTests = computeTestDeployment(result);

		// Update also the shared data structure
		for (Entry<TestJob, Image> mapping : deploymentOfTests.entrySet()) {
		    mapping.getKey().runWith = mapping.getValue();
		}

		List<Entry<Image, Image>> snapshots = computeSnapshots(result);

		return new SimpleEntry<Map<TestJob, Image>, List<Entry<Image, Image>>>(deploymentOfTests, snapshots);
	    } catch (CycleFoundException e) {
		throw new RuntimeException("Wrong input: found cyclic dependency");
	    }

	    //
	} else {
	    throw new RuntimeException("CANNOT FIND A SOLUTION !");
	}
    }

    //
    @Override
    public Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> solve(Set<TestJob> testJobs,
	    Set<Image> availableImages, Image baseImage, CloudModel cloudModel, Goal goal) {

	this.testJobs = testJobs;
	this.availableImages = availableImages;
	this.baseImage = baseImage;
	this.cloudModel = cloudModel;
	this.goal = goal;

	//
	createTheProblem();

	//

	return solve();
    }

    // public static void main(String[] args) throws CycleFoundException {
    //
    // boolean gui = false;
    //
    // DataDriver d = DataDriver.createRealData();// createAnotherTestData();
    // // // createTestData();//
    // // createAnotherTestData();
    // // Keep less jobs
    //
    // Set<TestJob> testJobs = new HashSet<TestJob>();
    // Iterator<TestJob> iterator = d.testJobs.iterator();
    // int i = 0;
    // while (iterator.hasNext() && i < 100) {
    // i++;
    // testJobs.add(iterator.next());
    // }
    //
    // Set<Image> availableImages = d.availableImages;
    // CloudModel cloudModel = d.cloudModel;
    // Goal goal = d.goal;
    //
    // OpportunisticSnapshot os = new OpportunisticSnapshot();
    //
    // Thread t = null;
    // if (gui) {
    // OpportunisticSnapshotFlowModelApplet applet = new
    // OpportunisticSnapshotFlowModelApplet();
    // applet.oSnap = os.theModel;
    // t = new Thread(new Runnable() {
    // @Override
    // public void run() {
    // applet.visualize();
    // }
    // });
    // t.start();
    // }
    //
    // //
    // Entry<Map<TestJob, Image>, List<Entry<Image, Image>>> solution =
    // os.solve(testJobs, availableImages, null,
    // cloudModel, goal);
    // //
    // System.out.println(printSummaryResult(solution));
    //
    // if (gui) {
    // try {
    // if (t != null)
    // t.join();
    // } catch (InterruptedException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }
    // }

}
