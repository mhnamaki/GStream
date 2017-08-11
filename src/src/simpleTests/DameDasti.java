package src.simpleTests;

import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jgrapht.ListenableGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.jgrapht.graph.ListenableDirectedWeightedGraph;

import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableDirectedWeightedGraph;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;

public class DameDasti {

	private static void createAndShowGui() {
		JFrame frame = new JFrame("DemoGraph");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		ListenableGraph<String, MyEdge> g1 = buildGraph();
		// ListenableGraph<String, MyEdge> g2 = buildGraph();
		JGraphXAdapter<String, MyEdge> graphAdapter1 = new JGraphXAdapter<String, MyEdge>(g1);

		// JGraphXAdapter<String, MyEdge> graphAdapter2 = new
		// JGraphXAdapter<String, MyEdge>(g2);

		mxIGraphLayout layout1 = new mxHierarchicalLayout(graphAdapter1);
		layout1.execute(graphAdapter1.getDefaultParent());

		// mxIGraphLayout layout2 = new mxCircleLayout(graphAdapter2);
		// layout2.execute(graphAdapter2.getDefaultParent());

		frame.add(new mxGraphComponent(graphAdapter1));
		// frame.add(new mxGraphComponent(graphAdapter2));

		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		System.out.println("This is a Test for 123 *&^_".replaceAll("[^A-Za-z0-9 ]", ""));
		
		//createAndShowGui();
		//createAndShowGui();
		// SwingUtilities.invokeLater(new Runnable() {
		// public void run() {
		// createAndShowGui();
		// }
		// });
	}

	public static class MyEdge extends DefaultWeightedEdge {
		
		@Override
		public String toString() {
			return "";
		}
	}

	public static ListenableGraph<String, MyEdge> buildGraph() {
		ListenableDirectedGraph<String, MyEdge> g = new ListenableDirectedGraph<String, MyEdge>(MyEdge.class);

		String x1 = "x1";
		String x2 = "x2";
		String x3 = "x3";

		g.addVertex(x1);
		g.addVertex(x2);
		g.addVertex(x3);

		MyEdge e = g.addEdge(x1, x2);
		e = g.addEdge(x2, x3);
		e = g.addEdge(x3, x1);

		return g;
	}
}

// }
