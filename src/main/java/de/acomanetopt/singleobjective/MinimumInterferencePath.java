package de.acomanetopt.singleobjective;

import de.jgraphlib.graph.generator.NetworkGraphGenerator;
import de.jgraphlib.graph.generator.NetworkGraphProperties;

import java.util.function.Function;

import de.aco.alg.multiobjective.ACOMultiObjective;
import de.aco.alg.singleobjective.ACOSingleObjective;
import de.jgraphlib.graph.Path;
import de.jgraphlib.graph.Position2D;
import de.jgraphlib.graph.generator.GraphProperties.DoubleRange;
import de.jgraphlib.graph.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;
import de.acomanetopt.manetmodel.MANET;
import de.acomanetopt.manetmodel.ManetSupplier;
import de.acomanetopt.manetmodel.Node;
import de.acomanetopt.manetmodel.Link;
import de.acomanetopt.manetmodel.LinkQuality;
import de.acomanetopt.manetmodel.DataRate;
import de.acomanetopt.manetmodel.Flow;
import de.acomanetopt.manetmodel.IdealRadioModel;

public class MinimumInterferencePath {

	//@formatter:off

	private final MANET<Node, Link<LinkQuality>, LinkQuality> manet;
	
	public MinimumInterferencePath(MANET<Node, Link<LinkQuality>, LinkQuality> manet) {
		this.manet = manet;
	}
	
	public Path<Node, Link<LinkQuality>, LinkQuality> compute(){
				
		ACOSingleObjective<Node, Position2D, Link<LinkQuality>, LinkQuality> aco = 
				new ACOSingleObjective<Node, Position2D, Link<LinkQuality>, LinkQuality>(
						/*alpha*/ 		0.5, 
						/*beta*/		2, 
						/*evaporation*/	0.5, 
						/*ants*/		1000, 
						/*iterations*/	10);
		
		aco.setGraph(manet);	
		aco.setMetric((LinkQuality w) -> {return (double) w.getUtilizedLinks();});	
		aco.run();
			
		return aco.getSolution().getSolution();
	}
	
	public static void main(String args[]) {
				
		MANET<Node, Link<LinkQuality>, LinkQuality> manet = new MANET<Node, Link<LinkQuality>, LinkQuality>(
				new ManetSupplier().getNodeSupplier(), 
				new ManetSupplier().getLinkSupplier(), 
				new ManetSupplier().getLinkQualitySupplier(),
				new IdealRadioModel(50, 100, new DataRate(20)));

		NetworkGraphProperties properties = new NetworkGraphProperties(
				/* playground width */ 			1024,
				/* playground height */ 		768, 
				/* number of vertices */ 		new IntRange(100, 200),
				/* distance between vertices */ new DoubleRange(50d, 100d), 
				/* edge distance */ 			100);

		NetworkGraphGenerator<Node, Link<LinkQuality>, LinkQuality> generator = 
				new NetworkGraphGenerator<Node, Link<LinkQuality>, LinkQuality>(manet, new ManetSupplier().getLinkQualitySupplier(), new RandomNumbers());
		
		generator.generate(properties);
			
		ACOSingleObjective<Node, Position2D, Link<LinkQuality>, LinkQuality> aco = 
				new ACOSingleObjective<Node, Position2D, Link<LinkQuality>, LinkQuality>(
						/*alpha*/ 		0.5, 
						/*beta*/		2, 
						/*evaporation*/	0.5, 
						/*ants*/		1000, 
						/*iterations*/	10);	
		aco.setGraph(manet);			
		aco.setMetric((LinkQuality w) -> {return (double) w.getUtilizedLinks();});			
		aco.run();
		
		VisualGraphApp<Node, Link<LinkQuality>, LinkQuality> visualGraphApp = new VisualGraphApp<Node, Link<LinkQuality>, LinkQuality>(manet, null);		
		visualGraphApp.getVisualGraphFrame().getVisualGraphPanel().addVisualPath(aco.getSolution().getSolution());	
	}
}
