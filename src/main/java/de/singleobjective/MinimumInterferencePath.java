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
import de.manetmodel.network.LinkProperties;
import de.manetmodel.network.Manet;
import de.manetmodel.network.ManetSupplier;
import de.manetmodel.network.Node;
import de.manetmodel.network.radio.IdealRadioModel;
import de.manetmodel.network.unit.DataRate;

public class MinimumInterferencePath {

	//@formatter:off

	private final Manet<Node, Link<LinkProperties>, LinkProperties> manet;
	private final Node source;
	private final Node target;
	
	public MinimumInterferencePath(Manet<Node, Link<LinkProperties>, LinkProperties> manet, Node source, Node target) {
		this.manet = manet;
		this.source = source;
		this.target = target;
	}
	
	public Path<Node, Link<LinkProperties>, LinkProperties> compute(){
				
		ACOShortestPath<Node, Position2D, Link<LinkProperties>, LinkProperties> acoShortestPath = new ACOShortestPath<Node, Position2D, Link<LinkProperties>, LinkProperties>(
				/*network*/		manet,
				/*metric*/		(LinkProperties w) -> {return (double) w.getInterference();},
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
				
		Manet<Node, Link<LinkProperties>, LinkProperties> manet = new Manet<Node, Link<LinkProperties>, LinkProperties>(
				new ManetSupplier().getNodeSupplier(), new ManetSupplier().getLinkSupplier(),
				new IdealRadioModel(50, 100, new DataRate(10000)));

		NetworkGraphProperties properties = new NetworkGraphProperties(
				/* playground width */ 			1024,
				/* playground height */ 		768, 
				/* number of vertices */ 		new IntRange(100, 200),
				/* distance between vertices */ new DoubleRange(50d, 100d), 
				/* edge distance */ 			100);

		NetworkGraphGenerator<Node, Link<LinkProperties>, LinkProperties> generator = 
				new NetworkGraphGenerator<Node, Link<LinkProperties>, LinkProperties>(manet, new ManetSupplier().getLinkPropertySupplier());
		
		generator.generate(properties);
		
		VisualGraphApp<Node, Link<LinkProperties>, LinkProperties> visualGraphApp = new VisualGraphApp<Node, Link<LinkProperties>, LinkProperties>(manet, null);		
		
		Function<LinkProperties, Double> metric = (LinkProperties w) -> {
			return (double) w.getInterference();
		};
		
		ACOShortestPath<Node, Position2D, Link<LinkProperties>, LinkProperties> acoShortestPath = new ACOShortestPath<Node, Position2D, Link<LinkProperties>, LinkProperties>(
				/*network*/		manet,
				/*metric*/		metric,
				/*source*/ 		manet.getFirstVertex(),
				/*target*/ 		manet.getLastVertex(),
				/*ants*/		1000,
				/*iterations*/	10,
				/*threads*/		4,
				/*tasks*/		4);
		
		acoShortestPath.run();
		
		visualGraphApp.getVisualGraphFrame().getVisualGraphPanel().addVisualPath(acoShortestPath.getSolution().getSolution());	
	}
}
