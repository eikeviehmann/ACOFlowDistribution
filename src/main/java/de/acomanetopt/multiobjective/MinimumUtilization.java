package de.acomanetopt.multiobjective;

import java.util.List;
import de.aco.ACOSolution;
import de.aco.Ant;
import de.aco.AntConsumer;
import de.aco.AntRequirement;
import de.aco.alg.multiobjective.ACOMultiObjective;
import de.aco.amplifiers.TargetingAmplifier;
import de.acomanetopt.manetmodel.MANET;
import de.acomanetopt.manetmodel.ManetSupplier;
import de.acomanetopt.manetmodel.Node;
import de.acomanetopt.manetmodel.Link;
import de.acomanetopt.manetmodel.LinkQuality;
import de.acomanetopt.manetmodel.DataRate;
import de.acomanetopt.manetmodel.Flow;
import de.acomanetopt.manetmodel.IdealRadioModel;
import de.jgraphlib.graph.Path;
import de.jgraphlib.graph.Position2D;
import de.jgraphlib.graph.WeightedGraph;
import de.jgraphlib.graph.generator.NetworkGraphGenerator;
import de.jgraphlib.graph.generator.NetworkGraphProperties;
import de.jgraphlib.graph.generator.GraphProperties.DoubleRange;
import de.jgraphlib.graph.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;
import de.jgraphlib.util.Tuple;

public class MinimumUtilization {

	//@formatter:off
	
	private ACOMultiObjective<Node, Position2D, Link<LinkQuality>, LinkQuality> aco;
	private MANET<Node, Link<LinkQuality>, LinkQuality> manet;
		
	public MinimumUtilization(MANET<Node, Link<LinkQuality>, LinkQuality> manet) {
		this.manet = manet;
	}

	public void initialize() {
		
		ACOMultiObjective<Node, Position2D, Link<LinkQuality>, LinkQuality> aco = 
				new ACOMultiObjective<Node, Position2D, Link<LinkQuality>, LinkQuality>(
						/*alpha*/ 		0.5, 
						/*beta*/		1, 
						/*evaporation*/	0.5, 
						/*ants*/		2000, 
						/*iterations*/	10);
		
		aco.setGraph(manet);
		
		aco.setMetric((LinkQuality w) -> {return (double) w.getUtilizedLinks();});		
		
		// Requirement: Only choose from links that have enough bandwidth left
		aco.setAntRequirement(
			new AntRequirement<Node, Position2D, Link<LinkQuality>, LinkQuality> (){				
				@Override
				public boolean require(Ant<Node, Position2D, Link<LinkQuality>, LinkQuality> ant, WeightedGraph<Node, Position2D, Link<LinkQuality>, LinkQuality> graph) {
											
					MANET<Node, Link<LinkQuality>, LinkQuality> copy =  (MANET<Node, Link<LinkQuality>, LinkQuality>) graph;	
										
					for(Flow<Node, Link<LinkQuality>, LinkQuality> flow : copy.getFlows()) {
												
						for(Tuple<Link<LinkQuality>, Node> tuple : flow) {
											
							if(tuple.getFirst() != null) {
								
								if(tuple.getFirst().getWeight().getUtilization().get() + copy.getFlows().get(ant.getColonyID()).getDataRate().get() > tuple.getFirst().getWeight().getTransmissionRate().get()) {
									
									return false;
									
								}
								
							}				
						}				
					}
				
					return true;
				}
			});		
		
		// Consumer: Consume data rate of links in transmission range while building solution along the graph
		aco.setAntConsumer(
			new AntConsumer<Node, Position2D, Link<LinkQuality>, LinkQuality> (){
				@Override
				public void consume(Ant<Node, Position2D, Link<LinkQuality>, LinkQuality> ant, WeightedGraph<Node, Position2D, Link<LinkQuality>, LinkQuality> graph) {	
								
					MANET<Node, Link<LinkQuality>, LinkQuality> copy = (MANET<Node, Link<LinkQuality>, LinkQuality>) graph;	
																							
					copy.getFlows().get(ant.getColonyID()).add(ant.getPath().getLast());
										
					copy.increaseUtilizationBy(ant.getPath().getLastEdge(), copy.getFlows().get(ant.getColonyID()).getDataRate());
									
				}		
			});		
		
		aco.addAntAmplifier(new TargetingAmplifier<Node, Link<LinkQuality>, LinkQuality>(2));
		
		for(Flow<Node, Link<LinkQuality>, LinkQuality> flow : manet.getFlows()) aco.addObjective(flow);
		
		aco.initialize(1, 1);		
		
		this.aco = aco;
	}
	
	public void compute() {			
		
		aco.run();	
		
		ACOSolution<List<Path<Node, Link<LinkQuality>, LinkQuality>>, Double> result = aco.getSolution();
		
		if(result != null)	
			for(int i=0; i<result.getSolution().size(); i++) 
				manet.deployFlow(new Flow<Node,Link<LinkQuality>,LinkQuality>(result.getSolution().get(i), manet.getFlows().get(i).getDataRate()));		
	}
	
	public static void main(String args[]) {
				
		/**************************************************************************************************************************************/
		/* Generate a MANET network graph*/
		
		MANET<Node, Link<LinkQuality>, LinkQuality> manet = new MANET<Node, Link<LinkQuality>, LinkQuality>(
						new ManetSupplier().getNodeSupplier(), 
						new ManetSupplier().getLinkSupplier(), 
						new ManetSupplier().getLinkQualitySupplier(),
						new IdealRadioModel(50, 100, new DataRate(100)));

		NetworkGraphProperties properties = new NetworkGraphProperties(
				/* playground width */ 			1024,
				/* playground height */ 		768, 
				/* number of vertices */ 		new IntRange(50, 50),
				/* distance between vertices */ new DoubleRange(50d, 100d), 
				/* edge distance */ 			new DoubleRange(100d, 100d));
		
		NetworkGraphGenerator<Node, Link<LinkQuality>, LinkQuality> generator = 
				new NetworkGraphGenerator<Node, Link<LinkQuality>, LinkQuality>(manet, new ManetSupplier().getLinkQualitySupplier(), new RandomNumbers());
		
		generator.generate(properties);	
	
		/**************************************************************************************************************************************/
		/* Setup & compute MinimumUtilization optimization*/
						
		manet.addFlow(new Flow<Node, Link<LinkQuality>, LinkQuality>(manet.getVertex(0), manet.getVertices().get(new RandomNumbers().getRandom(0, manet.getVertices().size())), new DataRate(5)));
		manet.addFlow(new Flow<Node, Link<LinkQuality>, LinkQuality>(manet.getVertex(0), manet.getVertices().get(new RandomNumbers().getRandom(0, manet.getVertices().size())), new DataRate(2)));
		manet.addFlow(new Flow<Node, Link<LinkQuality>, LinkQuality>(manet.getVertex(0), manet.getVertices().get(new RandomNumbers().getRandom(0, manet.getVertices().size())), new DataRate(3)));
		manet.addFlow(new Flow<Node, Link<LinkQuality>, LinkQuality>(manet.getVertex(0), manet.getVertices().get(new RandomNumbers().getRandom(0, manet.getVertices().size())), new DataRate(3)));

		
		MinimumUtilization optimization = new MinimumUtilization(manet);		

		optimization.initialize();
		optimization.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */
					
		VisualGraphApp<Node, Link<LinkQuality>, LinkQuality> app = new VisualGraphApp<Node, Link<LinkQuality>, LinkQuality>(manet, null);	
		
		for(Flow<Node,Link<LinkQuality>,LinkQuality> flow : manet.getFlows()) 
			app.getVisualGraphFrame().getVisualGraphPanel().addVisualPath(flow);			
	}	
}
