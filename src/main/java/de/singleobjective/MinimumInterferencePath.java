package de.singleobjective;

import de.jgraphlib.graph.generator.NetworkGraphGenerator;
import de.jgraphlib.graph.generator.NetworkGraphProperties;

import java.util.function.Function;

import de.aco.algorithms.ACOShortestPath;
import de.jgraphlib.graph.Path;
import de.jgraphlib.graph.Position2D;
import de.jgraphlib.graph.generator.GraphProperties.DoubleRange;
import de.jgraphlib.graph.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.VisualGraphApp;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANET;
import de.manetmodel.network.ManetSupplier;
import de.manetmodel.network.Node;
import de.manetmodel.network.radio.IdealRadioModel;
import de.manetmodel.network.unit.DataRate;

public class MinimumInterferencePath {

	//@formatter:off

	private final MANET<Node, Link<LinkQuality>, LinkQuality> MANET;
	private final Node source;
	private final Node target;
	
	public MinimumInterferencePath(MANET<Node, Link<LinkQuality>, LinkQuality> MANET, Node source, Node target) {
		this.MANET = MANET;
		this.source = source;
		this.target = target;
	}
	
	public Path<Node, Link<LinkQuality>, LinkQuality> compute(){
				
		ACOShortestPath<Node, Position2D, Link<LinkQuality>, LinkQuality> acoShortestPath = new ACOShortestPath<Node, Position2D, Link<LinkQuality>, LinkQuality>(
				/*network*/		MANET,
				/*metric*/		(LinkQuality w) -> {return (double) w.getInterference();},
				/*source*/ 		source,
				/*target*/ 		target,
				/*ants*/		1000,
				/*iterations*/	10,
				/*threads*/		4,
				/*tasks*/		4);
		
		acoShortestPath.run();
		
		return acoShortestPath.getSolution().getSolution();
	}
	
	public static void main(String args[]) {
				
		MANET<Node, Link<LinkQuality>, LinkQuality> MANET = new MANET<Node, Link<LinkQuality>, LinkQuality>(
				new ManetSupplier().getNodeSupplier(), new ManetSupplier().getLinkSupplier(),
				new IdealRadioModel(50, 100, new DataRate(10000)));

		NetworkGraphProperties properties = new NetworkGraphProperties(
				/* playground width */ 			1024,
				/* playground height */ 		768, 
				/* number of vertices */ 		new IntRange(100, 200),
				/* distance between vertices */ new DoubleRange(50d, 100d), 
				/* edge distance */ 			100);

		NetworkGraphGenerator<Node, Link<LinkQuality>, LinkQuality> generator = 
				new NetworkGraphGenerator<Node, Link<LinkQuality>, LinkQuality>(MANET, new ManetSupplier().getLinkPropertySupplier());
		
		generator.generate(properties);
		
		VisualGraphApp<Node, Link<LinkQuality>, LinkQuality> visualGraphApp = new VisualGraphApp<Node, Link<LinkQuality>, LinkQuality>(MANET, null);		
		
		Function<LinkQuality, Double> metric = (LinkQuality w) -> {
			return (double) w.getInterference();
		};
		
		ACOShortestPath<Node, Position2D, Link<LinkQuality>, LinkQuality> acoShortestPath = new ACOShortestPath<Node, Position2D, Link<LinkQuality>, LinkQuality>(
				/*network*/		MANET,
				/*metric*/		metric,
				/*source*/ 		MANET.getFirstVertex(),
				/*target*/ 		MANET.getLastVertex(),
				/*ants*/		1000,
				/*iterations*/	10,
				/*threads*/		4,
				/*tasks*/		4);
		
		acoShortestPath.run();
		
		visualGraphApp.getVisualGraphFrame().getVisualGraphPanel().addVisualPath(acoShortestPath.getSolution().getSolution());	
	}
}
