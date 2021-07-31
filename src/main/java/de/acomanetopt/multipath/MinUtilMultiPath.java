package de.acomanetopt.multipath;

import de.aco.ACOSolution;
import de.aco.Ant;
import de.aco.AntConsumer;
import de.aco.AntGroup;
import de.aco.AntRequirement;
import de.aco.alg.multipath.ACOMultiPath;
import de.jgraphlib.graph.WeightedGraph;
import de.jgraphlib.graph.elements.Position2D;
import de.jgraphlib.graph.generator.NetworkGraphGenerator;
import de.jgraphlib.graph.generator.NetworkGraphProperties;
import de.jgraphlib.graph.generator.GraphProperties.DoubleRange;
import de.jgraphlib.graph.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;
import de.manetmodel.gui.LinkQualityPrinter;
import de.manetmodel.network.Flow;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANET;
import de.manetmodel.network.MANETSupplier;
import de.manetmodel.network.Node;
import de.manetmodel.network.mobility.PedestrianMobilityModel;
import de.manetmodel.network.radio.ScalarRadioModel;
import de.manetmodel.network.unit.DataRate;
import de.manetmodel.network.unit.Speed;
import de.manetmodel.network.unit.Time;
import de.manetmodel.network.unit.Unit;
import de.manetmodel.network.unit.Speed.SpeedRange;

public class MinUtilMultiPath {

	//@formatter:off
	
	private ACOMultiPath<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> aco;
	private MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> manet;
		
	public MinUtilMultiPath(MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> manet) {
		this.manet = manet;		
	}
	
	public void initialize() {
		
		aco = new ACOMultiPath<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>>(
						/*alpha*/ 		1, 
						/*beta*/		1, 
						/*evaporation*/	0.5, 
						/*ants*/		1000, 
						/*iterations*/	10);
		
		aco.setGraph(manet);	
		
		aco.setMetric((LinkQuality w) -> {return (double) w.getNumberOfUtilizedLinks();});		
		
		// AntRequirement: Ants can only choose from links that have enough data rate left
		aco.setAntRequirement(
			new AntRequirement<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> (){				
				@Override
				public boolean check(
						Ant<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> ant,
						Link<LinkQuality> link, 
						WeightedGraph<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> graph) {
											
					MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> copy = 
							(MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>>) graph;	
																				
					for(Link<LinkQuality> utilizedLink : copy.getActiveUtilizedLinksOf(link)) {														
						if(utilizedLink.getWeight().getUtilization().get() + ant.getPath().getDataRate().get() > utilizedLink.getWeight().getTransmissionRate().get()) {							
							//System.out.println("OVERUTILIZED");				
							return false;			
						}
					}
								
					return true;
				}
			});		
		
		// AntConsumer: Ants consume data rate of links in transmission range while building solution (traversing) along the graph
		aco.setAntConsumer(
			new AntConsumer<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> (){
				@Override
				public void consume(Ant<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> ant, 
						WeightedGraph<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> graph) {	
										
					MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> copy = 
							(MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>>) graph;
															
					copy.increaseUtilizationBy(ant.getPath().getLastEdge(), copy.getFlows().get(ant.getColonyID()).getDataRate());								
				}		
			});		
		
		//aco.addAntAmplifier(new TargetingAmplifier<Node, Link<LinkQuality>, LinkQuality>(2));	
		
		aco.initialize(1, 1);				
	}
	
	public void compute() {			
		
		aco.run();	
		
		ACOSolution<AntGroup<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>>, Double> solution = aco.getSolution();
		
		if(aco.foundSolution())	{
			
			manet.clearFlows();
			
			for(Ant<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> ant : solution.getSolution()) {
				manet.addFlow(ant.getPath());
				manet.deployFlow(ant.getPath());		
			}			
		}			
	}
	
	public static void main(String args[]) {
				
		/**************************************************************************************************************************************/
		/* Import MANET network graph*/
		
		MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> manet = 
				new MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>>(
					new MANETSupplier().getNodeSupplier(), 
					new MANETSupplier().getLinkSupplier(),
					new MANETSupplier().getLinkQualitySupplier(),
					new MANETSupplier().getFlowSupplier(),
					new ScalarRadioModel(0.002d, 1e-11, 1000d, 2412000000d), 
					new PedestrianMobilityModel(RandomNumbers.getInstance(10),
						new SpeedRange(4d, 40d, Unit.Time.hour, Unit.Distance.kilometer),
						new Time(Unit.Time.second, 30l),
						new Speed(4d, Unit.Distance.kilometer, Unit.Time.hour), 10));
							
		NetworkGraphProperties properties = new NetworkGraphProperties(
				/* playground width */ 			1024,
				/* playground height */ 		768, 
				/* number of vertices */ 		new IntRange(100, 100),
				/* distance between vertices */ new DoubleRange(50d, 100d),
				/* edge distance */ 			new DoubleRange(100d, 100d));

		NetworkGraphGenerator<Node, Link<LinkQuality>, LinkQuality> generator = new NetworkGraphGenerator<Node, Link<LinkQuality>, LinkQuality>(
			manet, 
			new MANETSupplier().getLinkQualitySupplier(), 
			new RandomNumbers());

		generator.generate(properties);
		
		manet.initialize();
					
		/**************************************************************************************************************************************/
		/* Setup & compute */
				
		RandomNumbers random = new RandomNumbers();	
		int randomFlows = 3;
		
		for(int i=0; i < randomFlows; i++)
			manet.addFlow(new Flow<Node, Link<LinkQuality>, LinkQuality>(
				manet.getVertices().get(random.getRandom(0, manet.getVertices().size())), 
				manet.getVertices().get(random.getRandom(0, manet.getVertices().size())), new DataRate((random.getRandom(100, 500)))));	
				
		MinUtilMultiPath minUtilMultiPath = new MinUtilMultiPath(manet);		
		minUtilMultiPath.initialize();
		minUtilMultiPath.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */

		VisualGraphApp<Node, Link<LinkQuality>, LinkQuality> visualGraphApp = new VisualGraphApp<Node, Link<LinkQuality>, LinkQuality>(manet, new LinkQualityPrinter());
	}	
}
