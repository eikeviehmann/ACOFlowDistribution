package de.acomanetopt.singlepath;

import de.jgraphlib.graph.elements.Position2D;
import de.jgraphlib.graph.generator.NetworkGraphGenerator;
import de.jgraphlib.graph.generator.NetworkGraphProperties;
import de.aco.ACOSolution;
import de.aco.Ant;
import de.aco.alg.singlepath.ACOSinglePath;
import de.jgraphlib.graph.generator.GraphProperties.DoubleRange;
import de.jgraphlib.graph.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;
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


public class MinUtilSinglePath {

	//@formatter:off

	private ACOSinglePath<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> aco;
	private MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> manet;	
	
	public MinUtilSinglePath(MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>,LinkQuality>> manet) {
		this.manet = manet;
	}
	
	public void initialize() {
		
		aco = new ACOSinglePath<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>>(
						/*alpha*/ 		1, 
						/*beta*/		1, 
						/*evaporation*/	0.5, 
						/*ants*/		1000, 
						/*iterations*/	10);
		
		aco.setGraph(manet);	
		
		aco.setMetric((LinkQuality w) -> {return (double) w.getNumberOfUtilizedLinks();});		
	
		//aco.addAntAmplifier(new TargetingAmplifier<Node, Link<LinkQuality>, LinkQuality>(2));	
		
		aco.initialize(1, 1);				
	}
	
	public void compute() {			
		
		aco.run();	
		
		ACOSolution<Ant<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>>, Double> solution = aco.getSolution();
		
		if(aco.foundSolution())	{		
			manet.clearFlows();		
			Ant<Node, Position2D, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> ant = solution.getSolution();
			manet.addFlow(ant.getPath());
			manet.deployFlow(ant.getPath());					
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
						
		manet.addFlow(
				manet.getVertices().get(new RandomNumbers().getRandom(0, manet.getVertices().size())), 
				manet.getVertices().get(new RandomNumbers().getRandom(0, manet.getVertices().size())), 
				new DataRate(10));	
				
		MinUtilSinglePath minUtilMultiPath = new MinUtilSinglePath(manet);		
		minUtilMultiPath.initialize();
		minUtilMultiPath.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */

		VisualGraphApp<Node, Link<LinkQuality>, LinkQuality> visualGraphApp = new VisualGraphApp<Node, Link<LinkQuality>, LinkQuality>(manet, null);			
	}
}
