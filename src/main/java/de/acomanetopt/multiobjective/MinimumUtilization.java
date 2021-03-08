package de.acomanetopt.multiobjective;

import java.util.ArrayList;
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
	private List<Flow<Node, Link<LinkQuality>, LinkQuality>> flows;
		
	public MinimumUtilization(MANET<Node, Link<LinkQuality>, LinkQuality> manet) {
		this.manet = manet;
		this.flows = new ArrayList<Flow<Node, Link<LinkQuality>, LinkQuality>>();
	}

	public void addFlowObjective(Node source, Node target, DataRate bitrate) {	
		Flow<Node, Link<LinkQuality>, LinkQuality> flow = new Flow<Node, Link<LinkQuality>, LinkQuality>(source, target, bitrate);
		this.flows.add(flow);	
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
		
		// Requirement: Only choose from links that have enough bandwidth left
		aco.setAntRequirement(
			new AntRequirement<Node, Position2D, Link<LinkQuality>, LinkQuality> (){				
				@Override
				public boolean require(Ant<Node, Position2D, Link<LinkQuality>, LinkQuality> ant, LinkQuality w) {
						
					/* HERES THE MAGIC */ 
					
					return w.getUtilization().get() + flows.get(ant.getColonyID()).getDataRate().get() <= w.getTransmissionRate().get();	
					
				}		
			});		
		
		// Consumer: Consume data rate of links in transmission range while building solution along the graph
		aco.setAntConsumer(
			new AntConsumer<Node, Position2D, Link<LinkQuality>, LinkQuality> (){
				@Override
				public void consume(Ant<Node, Position2D, Link<LinkQuality>, LinkQuality> ant, UndirectedWeightedGraph<Node, Position2D, Link<LinkQuality>, LinkQuality> graph) {	
					
					/* HERES THE MAGIC */ 
					
					MANET<Node, Link<LinkQuality>, LinkQuality> tempManet =  (MANET<Node, Link<LinkQuality>, LinkQuality>) graph;							
					tempManet.increaseUtilizationBy(ant.getPath().getLastEdge(), flows.get(ant.getColonyID()).getDataRate());
						
				}		
			});		
		
		for(Flow<Node, Link<LinkQuality>, LinkQuality> flow : flows) aco.addObjective(flow);
		
		aco.initialize(1, 1);		
		
		this.aco = aco;
	}
	
	public void compute() {			
		
		aco.run();	
		
		ACOSolution<List<Path<Node, Link<LinkQuality>, LinkQuality>>, Double> result = aco.getSolution();
		
		if(result != null)	
			for(int i=0; i<result.getSolution().size(); i++) 
				manet.deployFlow(new Flow<Node,Link<LinkQuality>,LinkQuality>(result.getSolution().get(i), flows.get(i).getDataRate()));		
	}
	
	public static void main(String args[]) {
				
		/**************************************************************************************************************************************/
		/* Generate a MANET network graph*/
		
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
				new NetworkGraphGenerator<Node, Link<LinkQuality>, LinkQuality>(manet, new ManetSupplier().getLinkQualitySupplier());
		
		generator.generate(properties);	
	
		/**************************************************************************************************************************************/
		/* Setup & compute MinimumUtilization optimization*/
						
		MinimumUtilization optimization = new MinimumUtilization(manet);		
		optimization.addFlowObjective(manet.getVertex(0), manet.getVertices().get(RandomNumbers.getRandom(0, manet.getVertices().size())), new DataRate(1));			
		optimization.addFlowObjective(manet.getVertex(0), manet.getVertices().get(RandomNumbers.getRandom(0, manet.getVertices().size())), new DataRate(1));	
		optimization.addFlowObjective(manet.getVertex(0), manet.getVertices().get(RandomNumbers.getRandom(0, manet.getVertices().size())), new DataRate(1));			

		optimization.initialize();
		optimization.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */
					
		VisualGraphApp<Node, Link<LinkQuality>, LinkQuality> app = new VisualGraphApp<Node, Link<LinkQuality>, LinkQuality>(manet, null);	
		
		for(Flow<Node,Link<LinkQuality>,LinkQuality> flow : manet.getFlows()) 
			app.getVisualGraphFrame().getVisualGraphPanel().addVisualPath(flow);			
	}	
}
