package de.acomanetopt.multiobjective;

import java.util.List;
import de.aco.ACOSolution;
import de.aco.Ant;
import de.aco.alg.multiobjective.ACOMultiObjective;
import de.aco.alg.multiobjective.AntConsumer;
import de.aco.alg.multiobjective.AntRequirement;
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
import de.jgraphlib.graph.UndirectedWeightedGraph;
import de.jgraphlib.graph.generator.NetworkGraphGenerator;
import de.jgraphlib.graph.generator.NetworkGraphProperties;
import de.jgraphlib.graph.generator.GraphProperties.DoubleRange;
import de.jgraphlib.graph.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;

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
						/*beta*/		2, 
						/*evaporation*/	0.5, 
						/*ants*/		1000, 
						/*iterations*/	10);
		
		aco.setGraph(manet);
		
		aco.setMetric((LinkQuality w) -> {return (double) w.getUtilizedLinks();});		
		
		// Requirement: Only choose from Links that have enough bandwidth left
		aco.setAntRequirement(
			new AntRequirement<Node, Position2D, Link<LinkQuality>, LinkQuality> (){				
				@Override
				public boolean require(Ant<Node, Position2D, Link<LinkQuality>, LinkQuality> ant, LinkQuality w) {
					
					//DEBUG
					//System.out.format("Utilization %d of %d\n", w.getUtilization().get(), w.getTransmissionRate().get());
					//System.out.format("Flow demands: %d\n\n", manet.getFlows().get(ant.getColonyID()).getDataRate().get());
					
					return w.getUtilization().get() + manet.getFlows().get(ant.getColonyID()).getDataRate().get() <= w.getTransmissionRate().get();	
				}		
			});		
		
		// Consumer: Consume data rate of links in transmission range while building solution along the MANET
		aco.setAntConsumer(
			new AntConsumer<Node, Position2D, Link<LinkQuality>, LinkQuality> (){
				@Override
				public void consume(Ant<Node, Position2D, Link<LinkQuality>, LinkQuality> ant, UndirectedWeightedGraph<Node, Position2D, Link<LinkQuality>, LinkQuality> graph) {							
					
					MANET<Node, Link<LinkQuality>, LinkQuality> tempManet =  (MANET<Node, Link<LinkQuality>, LinkQuality>) graph;
					
					//System.out.format("MANET utilization: %d of", tempManet.getUtilization().get());
							
					tempManet.increaseUtilizationBy(ant.getPath().getLastEdge(), manet.getFlows().get(ant.getColonyID()).getDataRate());
															
					//System.out.format(" %d\n", tempManet.getUtilization().get());
				}		
			});		
		
		for(Flow<Node, Link<LinkQuality>, LinkQuality> flow : manet.getFlows()) aco.addObjective(flow);
		
		aco.initialize(1, 1);		
		
		this.aco = aco;
	}
	
	public void compute() {			
		aco.run();	
	}
	
	public List<Path<Node, Link<LinkQuality>, LinkQuality>> getSolution() {
					
		ACOSolution<List<Path<Node, Link<LinkQuality>, LinkQuality>>, Double> result = aco.getSolution();	
			
		if(result != null)
			return result.getSolution();
		
		return null;
	}
	
	public static void main(String args[]) {
				
		/**************************************************************************************************************************************/
		/* Generate a MANET network graph*/
		
		MANET<Node, Link<LinkQuality>, LinkQuality> manet = new MANET<Node, Link<LinkQuality>, LinkQuality>(
						new ManetSupplier().getNodeSupplier(), 
						new ManetSupplier().getLinkSupplier(), 
						new ManetSupplier().getFlowSupplier(),
						new IdealRadioModel(50, 100, new DataRate(10000000)));

		NetworkGraphProperties properties = new NetworkGraphProperties(
				/* playground width */ 			1024,
				/* playground height */ 		768, 
				/* number of vertices */ 		new IntRange(100, 200),
				/* distance between vertices */ new DoubleRange(50d, 100d), 
				/* edge distance */ 			100);

		NetworkGraphGenerator<Node, Link<LinkQuality>, LinkQuality> generator = 
				new NetworkGraphGenerator<Node, Link<LinkQuality>, LinkQuality>(manet, new ManetSupplier().getLinkPropertySupplier());
		
		generator.generate(properties);
		
		manet.addFlow(manet.getVertex(0), manet.getVertices().get(RandomNumbers.getRandom(0, manet.getVertices().size())), new DataRate(100));			
		manet.addFlow(manet.getVertex(0), manet.getVertices().get(RandomNumbers.getRandom(0, manet.getVertices().size())), new DataRate(100));
	
		/**************************************************************************************************************************************/
		/* Setup & compute MinimumUtilization optimization*/
						
		MinimumUtilization optimization = new MinimumUtilization(manet);
			
		optimization.initialize();
		optimization.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */
		
		List<Path<Node, Link<LinkQuality>, LinkQuality>> paths = optimization.getSolution();
		
		if(paths != null) {
			
			VisualGraphApp<Node, Link<LinkQuality>, LinkQuality> app = new VisualGraphApp<Node, Link<LinkQuality>, LinkQuality>(manet, null);	
		
			for(Path<Node, Link<LinkQuality>, LinkQuality> path : paths)
				app.getVisualGraphFrame().getVisualGraphPanel().addVisualPath(path);
		}
	}	
}
