package src.utilities;
 
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.JGraphXAdapter;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxFastOrganicLayout;

import com.google.common.collect.MinMaxPriorityQueue;

import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;

import src.alg1.prefixTree.PrefixTreeNodeDataAlg1;
import src.base.IPrefixTree;
import src.base.IPrefixTreeNodeData;

//import src.prefixTree.DefaultLabeledEdge;
//import src.prefixTree.PatternNode;
//import src.prefixTree.PrefixTreeNode;
//import src.prefixTree.PrefixTreeNodeData;

public class Visualizer {

	public static void visualizeTopK(MinMaxPriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns) {
		if(!Dummy.DummyProperties.visualize)
			return;
		
		PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> newTopkQ = new PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>>(
				topKFrequentPatterns.size(), new SupportComparator());

		newTopkQ.addAll(topKFrequentPatterns);

		createAndShowGui(newTopkQ);

	}

	public static void visualizeALevel(IPrefixTree prefixTree, int level, int cntVisualization) {
		if(!Dummy.DummyProperties.visualize)
			return;
		
		System.out.println("level:" + level);
		ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> patternsInALevel = new ArrayList<PrefixTreeNode<IPrefixTreeNodeData>>();
		for (Integer patternId : prefixTree.getPrefixTreeNodeIndex().keySet()) {
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode = prefixTree.getPrefixTreeNodeIndex().get(patternId);

			if (prefixTreeNode.getLevel() == level) {
				patternsInALevel.add(prefixTreeNode);
			}

		}
		createAndShowGui2(patternsInALevel, level, cntVisualization);

	}

	public static void visualizeXnotInY(String title, ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> patterns) {
		if(!Dummy.DummyProperties.visualize)
			return;
		
		HashSet<PrefixTreeNode<IPrefixTreeNodeData>> patterns2 = new HashSet<PrefixTreeNode<IPrefixTreeNodeData>>();
		if (patterns.size() > 0) {
			patterns2.addAll(patterns);
			createAndShowGui3(title, patterns2);
		}
	}

	public static void visualizePatternWithDuplicateMatches(IPrefixTree prefixTree) {
		if(!Dummy.DummyProperties.visualize)
			return;
		
		HashSet<PrefixTreeNode<IPrefixTreeNodeData>> patterns = new HashSet<PrefixTreeNode<IPrefixTreeNodeData>>();

		for (Integer patternId : prefixTree.getPrefixTreeNodeIndex().keySet()) {
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode = prefixTree.getPrefixTreeNodeIndex().get(patternId);
			ArrayList<PatternNode> patternNodesArr = new ArrayList<PatternNode>();
			patternNodesArr.addAll(prefixTreeNode.getData().getPatternGraph().vertexSet());
			for (int i = 0; i < patternNodesArr.size(); i++) {
				for (int j = 0; j < patternNodesArr.size(); j++) {
					if (i < j) {
						PatternNode patternNode1 = patternNodesArr.get(i);
						PatternNode patternNode2 = patternNodesArr.get(j);
						if (prefixTreeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
								.get(patternNode1).size() != prefixTreeNode.getData().getMatchedNodes()
										.getDataGraphMatchNodeOfAbsPNode().get(patternNode2).size())
							continue;

						boolean sameMatches = true;
						for (Integer nodeId1 : prefixTreeNode.getData().getMatchedNodes()
								.getDataGraphMatchNodeOfAbsPNode().get(patternNode1)) {
							boolean isFoundEqual = false;
							for (Integer nodeId2 : prefixTreeNode.getData().getMatchedNodes()
									.getDataGraphMatchNodeOfAbsPNode().get(patternNode2)) {
								if (nodeId1 == nodeId2) {
									isFoundEqual = true;
									break;
								}
							}
							if (!isFoundEqual) {
								sameMatches = false;
								break;
							}
						}
						if (sameMatches) {
							patterns.add(prefixTreeNode);
						}
					}
				}
			}
		}
		createAndShowGui3("same matches (duplicated)", patterns);
	}

	private static void createAndShowGui3(String title, HashSet<PrefixTreeNode<IPrefixTreeNodeData>> patterns) {
		JFrame frame = new JFrame(title + " size:" + patterns.size());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		// prefixTreeNode.getData().patternPrefixTreeNodeIndex.toString()
		Iterator<PrefixTreeNode<IPrefixTreeNodeData>> itr = patterns.iterator();

		while (itr.hasNext()) {
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode = itr.next();

			JGraphXAdapter<PatternNode, DefaultLabeledEdge> graphAdapter1 = new JGraphXAdapter<PatternNode, DefaultLabeledEdge>(
					prefixTreeNode.getData().getPatternGraph());

			mxIGraphLayout layout1 = new mxHierarchicalLayout(graphAdapter1);
			layout1.execute(graphAdapter1.getDefaultParent());

			JPanel prefixNodePanel = new JPanel();
			prefixNodePanel.setLayout(new BoxLayout(prefixNodePanel, BoxLayout.Y_AXIS));

			JPanel titlePanel = new JPanel();
			JLabel titleLabel = new JLabel(Integer.toString(prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()));
			titlePanel.add(titleLabel);

			JPanel gPanel = new JPanel();
			gPanel.add(new mxGraphComponent(graphAdapter1));
			// gPanel.setLayout(new BorderLayout(10,10));

			gPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			// gPanel.setPreferredSize(new Dimension(gPanel.getSize().width+20,
			// gPanel.getSize().height+20));

			JTextArea textArea = new JTextArea(10, 20);
			JScrollPane scrollInfoPane = new JScrollPane(textArea);
			textArea.setEditable(false);

			String text = prefixTreeNode.getData().getPatternPrefixTreeNodeIndex() + "\n";

			for (PatternNode patternNode : prefixTreeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
					.keySet()) {
				text += patternNode + ": "
						+ prefixTreeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
						+ "\n";
			}

			text += "TotalSupp: " + prefixTreeNode.getData().getTotalSupportFrequency() + "\n" + "All Supp: "
					+ Arrays.toString(prefixTreeNode.getData().getSupportFrequencies());
			text += "\n dangling:" + prefixTreeNode.getData().isDanglingPattern() + " direction:"
					+ prefixTreeNode.getData().getGrowthDirection();
			text += "\n ancestors:";

			text += "\n " + prefixTreeNode.getLevel() + ":" + prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()
					+ prefixTreeNode.getData().getSourcePatternNode() + " -> "
					+ prefixTreeNode.getData().getTargetPatternNode() + " dangling:"
					+ prefixTreeNode.getData().isDanglingPattern() + " direction:"
					+ prefixTreeNode.getData().getGrowthDirection();
			while (prefixTreeNode.getParent() != null) {
				prefixTreeNode = prefixTreeNode.getParent();
				text += "\n " + prefixTreeNode.getLevel() + ":"
						+ prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()
						+ prefixTreeNode.getData().getSourcePatternNode() + " -> "
						+ prefixTreeNode.getData().getTargetPatternNode() + " dangling:"
						+ prefixTreeNode.getData().isDanglingPattern() + " direction:"
						+ prefixTreeNode.getData().getGrowthDirection();

			}

			textArea.setText(text);

			prefixNodePanel.add(titlePanel);
			prefixNodePanel.add(gPanel);
			prefixNodePanel.add(scrollInfoPane);

			// prefixNodePanel.setSize(prefixNodePanel.getSize().width + 20,
			// prefixNodePanel.getSize().height + 20);

			mainPanel.add(prefixNodePanel);

		}

		// frame.add(new mxGraphComponent(graphAdapter1));

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.add(mainPanel);
		scrollPane.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, 500);

		frame.getContentPane().add(scrollPane);

		frame.pack();

		frame.setLocationByPlatform(true);
		frame.setVisible(true);

	}

	private static void createAndShowGui2(ArrayList<PrefixTreeNode<IPrefixTreeNodeData>> patternsInALevel, int level,
			int cntVisualization) {
		JFrame frame = new JFrame("Level: " + level + " index:" + cntVisualization + " #: " + patternsInALevel.size());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		// prefixTreeNode.getData().patternPrefixTreeNodeIndex.toString()
		while (!patternsInALevel.isEmpty()) {
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode = patternsInALevel.remove(0);

			JGraphXAdapter<PatternNode, DefaultLabeledEdge> graphAdapter1 = new JGraphXAdapter<PatternNode, DefaultLabeledEdge>(
					prefixTreeNode.getData().getPatternGraph());

			mxIGraphLayout layout1 = new mxHierarchicalLayout(graphAdapter1);
			layout1.execute(graphAdapter1.getDefaultParent());

			JPanel prefixNodePanel = new JPanel();
			prefixNodePanel.setLayout(new BoxLayout(prefixNodePanel, BoxLayout.Y_AXIS));

			JPanel titlePanel = new JPanel();
			JLabel titleLabel = new JLabel(Integer.toString(prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()));
			titlePanel.add(titleLabel);

			JPanel gPanel = new JPanel();
			gPanel.add(new mxGraphComponent(graphAdapter1));
			// gPanel.setLayout(new BorderLayout(10,10));

			gPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			// gPanel.setPreferredSize(new Dimension(gPanel.getSize().width+20,
			// gPanel.getSize().height+20));

			JTextArea textArea = new JTextArea(10, 20);
			JScrollPane scrollInfoPane = new JScrollPane(textArea);
			textArea.setEditable(false);

			String text = prefixTreeNode.getData().getPatternPrefixTreeNodeIndex() + "\n";

			for (PatternNode patternNode : prefixTreeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
					.keySet()) {
				text += patternNode + ": "
						+ prefixTreeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
						+ "\n";
			}

			text += "TotalSupp: " + prefixTreeNode.getData().getTotalSupportFrequency() + "\n" + "All Supp: "
					+ Arrays.toString(prefixTreeNode.getData().getSupportFrequencies());
			text += "\n dangling:" + prefixTreeNode.getData().isDanglingPattern() + " direction:"
					+ prefixTreeNode.getData().getGrowthDirection();
			text += "\n ancestors:";

			text += "\n " + prefixTreeNode.getLevel() + ":" + prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()
					+ prefixTreeNode.getData().getSourcePatternNode() + " -> "
					+ prefixTreeNode.getData().getTargetPatternNode() + " dangling:"
					+ prefixTreeNode.getData().isDanglingPattern() + " direction:"
					+ prefixTreeNode.getData().getGrowthDirection();
			while (prefixTreeNode.getParent() != null) {
				prefixTreeNode = prefixTreeNode.getParent();
				text += "\n " + prefixTreeNode.getLevel() + ":"
						+ prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()
						+ prefixTreeNode.getData().getSourcePatternNode() + " -> "
						+ prefixTreeNode.getData().getTargetPatternNode() + " dangling:"
						+ prefixTreeNode.getData().isDanglingPattern() + " direction:"
						+ prefixTreeNode.getData().getGrowthDirection();

			}

			textArea.setText(text);

			prefixNodePanel.add(titlePanel);
			prefixNodePanel.add(gPanel);
			prefixNodePanel.add(scrollInfoPane);

			// prefixNodePanel.setSize(prefixNodePanel.getSize().width + 20,
			// prefixNodePanel.getSize().height + 20);

			mainPanel.add(prefixNodePanel);

		}

		// frame.add(new mxGraphComponent(graphAdapter1));

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.add(mainPanel);
		scrollPane.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, 500);

		frame.getContentPane().add(scrollPane);

		frame.pack();

		frame.setLocationByPlatform(true);
		frame.setVisible(true);

	}

	private static void createAndShowGui(PriorityQueue<PrefixTreeNode<IPrefixTreeNodeData>> topKFrequentPatterns) {

		JFrame frame = new JFrame("TOP-K FINAL RESULTS #:" + topKFrequentPatterns.size());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		// prefixTreeNode.getData().patternPrefixTreeNodeIndex.toString()
		while (!topKFrequentPatterns.isEmpty()) {
			PrefixTreeNode<IPrefixTreeNodeData> prefixTreeNode = topKFrequentPatterns.poll();

			JGraphXAdapter<PatternNode, DefaultLabeledEdge> graphAdapter1 = new JGraphXAdapter<PatternNode, DefaultLabeledEdge>(
					prefixTreeNode.getData().getPatternGraph());

			mxIGraphLayout layout1 = new mxHierarchicalLayout(graphAdapter1);
			layout1.execute(graphAdapter1.getDefaultParent());

			JPanel prefixNodePanel = new JPanel();
			prefixNodePanel.setLayout(new BoxLayout(prefixNodePanel, BoxLayout.Y_AXIS));

			JPanel titlePanel = new JPanel();
			JLabel titleLabel = new JLabel(Integer.toString(prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()));
			titlePanel.add(titleLabel);

			JPanel gPanel = new JPanel();
			gPanel.add(new mxGraphComponent(graphAdapter1));
			// gPanel.setLayout(new BorderLayout(10,10));

			gPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			// gPanel.setPreferredSize(new Dimension(gPanel.getSize().width+20,
			// gPanel.getSize().height+20));

			JTextArea textArea = new JTextArea(10, 20);
			JScrollPane scrollInfoPane = new JScrollPane(textArea);
			textArea.setEditable(false);

			String text = prefixTreeNode.getData().getPatternPrefixTreeNodeIndex() + "\n";

			for (PatternNode patternNode : prefixTreeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
					.keySet()) {
				text += patternNode + ": "
						+ prefixTreeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
						+ "\n";
			}

			text += "TotalSupp: " + prefixTreeNode.getData().getTotalSupportFrequency() + "\n" + "All Supp: "
					+ Arrays.toString(prefixTreeNode.getData().getSupportFrequencies());
			text += "\n dangling:" + prefixTreeNode.getData().isDanglingPattern();
			text += "\n ancestors:";

			text += "\n " + prefixTreeNode.getLevel() + ":" + prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()
					+ prefixTreeNode.getData().getSourcePatternNode() + " -> "
					+ prefixTreeNode.getData().getTargetPatternNode() + " dangling:"
					+ prefixTreeNode.getData().isDanglingPattern() + " direction:"
					+ prefixTreeNode.getData().getGrowthDirection();
			while (prefixTreeNode.getParent() != null) {
				prefixTreeNode = prefixTreeNode.getParent();
				text += "\n " + prefixTreeNode.getLevel() + ":"
						+ prefixTreeNode.getData().getPatternPrefixTreeNodeIndex()
						+ prefixTreeNode.getData().getSourcePatternNode() + " -> "
						+ prefixTreeNode.getData().getTargetPatternNode() + " dangling:"
						+ prefixTreeNode.getData().isDanglingPattern() + " direction:"
						+ prefixTreeNode.getData().getGrowthDirection();

			}

			textArea.setText(text);

			prefixNodePanel.add(titlePanel);
			prefixNodePanel.add(gPanel);
			prefixNodePanel.add(scrollInfoPane);

			// prefixNodePanel.setSize(prefixNodePanel.getSize().width + 20,
			// prefixNodePanel.getSize().height + 20);

			mainPanel.add(prefixNodePanel);

		}

		// frame.add(new mxGraphComponent(graphAdapter1));

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.add(mainPanel);
		scrollPane.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, 500);

		frame.getContentPane().add(scrollPane);

		frame.pack();

		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}

}
