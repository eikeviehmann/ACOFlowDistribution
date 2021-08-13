package de.manetacoeval;

import de.jgraphlib.graph.generator.NetworkGraphGenerator;
import de.jgraphlib.graph.generator.NetworkGraphProperties;
import de.aco.alg.singlepath.SinglePath;
import de.jgraphlib.graph.generator.GraphProperties.DoubleRange;
import de.jgraphlib.graph.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.EdgeIDPrinter;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;
import de.manetacoeval.model.oMANET;
import de.manetmodel.gui.LinkQualityPrinter;
import de.manetmodel.network.Flow;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANETSupplier;
import de.manetmodel.network.Node;
import de.manetmodel.network.mobility.PedestrianMobilityModel;
import de.manetmodel.network.radio.ScalarRadioModel;
import de.manetmodel.network.unit.DataRate;
import de.manetmodel.network.unit.Speed;
import de.manetmodel.network.unit.Time;
import de.manetmodel.network.unit.Unit;
import de.manetmodel.network.unit.Speed.SpeedRange;


public class FlowDistribution {

	//@formatter:off

	private SinglePath<Node, Link<LinkQuality>, LinkQuality, oMANET> aco;
	private oMANET manet;	
	private Flow<Node, Link<LinkQuality>, LinkQuality> flow;
	
	public FlowDistribution(oMANET manet, Flow<Node, Link<LinkQuality>, LinkQuality> flow) {
		this.manet = manet;
		this.flow = flow;	
	}
	
	public void initialize() {
		
		aco = new SinglePath<Node, Link<LinkQuality>, LinkQuality, oMANET>(
			/*alpha*/ 		1, 
			/*beta*/		1, 
			/*evaporation*/	0.5, 
			/*ants*/		1, 
			/*iterations*/	1);
		
		aco.setGraph(manet);	
		
		aco.setMetric((LinkQuality w) -> {return (double) w.getNumberOfUtilizedLinks();});		
			
		aco.initialize(flow.getSource(), flow.getTarget(), manet);
		
		manet.initialize();
	}
	
	public void compute() {			
		
		aco.run();	
				
		if(aco.foundSolution())	{		
			flow.update(aco.getSolution().getSolution().getPath());			
			manet.deployFlow(flow);					
		}				
	}
	
	public static void main(String args[]) {
				
		/**************************************************************************************************************************************/
		/* Import MANET network graph*/
		
		oMANET manet = new oMANET(
				new MANETSupplier().getNodeSupplier(), 
				new MANETSupplier().getLinkSupplier(),
				new MANETSupplier().getLinkPropertySupplier(),
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
					new MANETSupplier().getLinkPropertySupplier(), 
					new RandomNumbers(1000));

		generator.generate(properties);
							
		/**************************************************************************************************************************************/
		/* Setup & compute */
						
		manet.addFlow(new Flow<Node, Link<LinkQuality>, LinkQuality>(
				manet.getVertices().get(new RandomNumbers().getRandom(0, manet.getVertices().size())),
				manet.getVertices().get(new RandomNumbers().getRandom(0, manet.getVertices().size())),
				new DataRate(10000)));
				
		FlowDistribution flowDistribution = new FlowDistribution(manet, manet.getFlow(0));		
		flowDistribution.initialize();
		flowDistribution.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */

		VisualGraphApp<Node, Link<LinkQuality>, LinkQuality> visualGraphApp 
			= new VisualGraphApp<Node, Link<LinkQuality>, LinkQuality>(manet, manet.getFlow(0), new EdgeIDPrinter());			
	}
}
