package de.unisaarland.cs.st.graph;

import java.awt.Dimension;

import javax.swing.JApplet;
import javax.swing.JFrame;

import org.jgrapht.ext.JGraphXAdapter;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.swing.mxGraphComponent;

public class OpportunisticSnapshotFlowModelApplet extends JApplet {

    private static final long serialVersionUID = 2202072534703043194L;
    private static final Dimension DEFAULT_SIZE = new Dimension(530, 320);
    private JGraphXAdapter<Object, MyWeightedEdge> jgxAdapter;

    public OpportunisticSnapshotNetworkFlowModel oSnap;

    /**
     * {@inheritDoc}
     */
    public void init() {

	jgxAdapter = new JGraphXAdapter<Object, MyWeightedEdge>(oSnap.getNetworkFlow());

	getContentPane().add(new mxGraphComponent(jgxAdapter));
	resize(DEFAULT_SIZE);

	mxCircleLayout layout = new mxCircleLayout(jgxAdapter);
	layout.execute(jgxAdapter.getDefaultParent());

    }

    public void visualize() {
	init();
	//
	JFrame frame = new JFrame();
	frame.getContentPane().add(this);
	frame.setTitle("JGraphT Adapter to JGraph Demo");
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.pack();
	frame.setVisible(true);
    }

}
