package de.manetacoeval;

import java.util.Collections;
import java.util.stream.Collectors;

import de.aco.alg.multipath.RoundRobinMultiPath;
import de.aco.amplifiers.TargetingAmplifier;
import de.jgraphlib.graph.elements.EdgeDistance;
import de.jgraphlib.graph.elements.Position2D;
import de.jgraphlib.graph.elements.Vertex;
import de.jgraphlib.graph.elements.WeightedEdge;
import de.jgraphlib.graph.generator.NetworkGraphGenerator;
import de.jgraphlib.graph.generator.NetworkGraphProperties;
import de.jgraphlib.graph.generator.GraphProperties.DoubleRange;
import de.jgraphlib.graph.generator.GraphProperties.IntRange;
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

public class MultiFlowDistribution {

	//@formatter:off
	
	private RoundRobinMultiPath<Node, Link<LinkQuality>, LinkQuality, oMANET> aco;
	private oMANET manet;
		
	public MultiFlowDistribution(oMANET manet) {
		this.manet = manet;		
	}
	
	public void initialize() {
		
		aco = new RoundRobinMultiPath<Node, Link<LinkQuality>, LinkQuality, oMANET>(
				/*alpha*/ 		1, 
				/*beta*/		1, 
				/*evaporation*/	0.5, 
				/*ants*/		10000, 
				/*iterations*/	10);
		
		aco.setGraph(manet);	
		
		aco.setMetric((LinkQuality w) -> {return (double) w.getNumberOfUtilizedLinks();});		
					
		aco.initialize(
				/* source sink tuples*/	manet.getFlows().stream().map(flow -> flow.getSourceTargetTuple()).collect(Collectors.toList()), 
				/* 4 threads*/			8, 
				/* 4 tasks*/			8, 
				/* 4 model copies*/		Collections.nCopies(8, manet.copy()));	
		
		//aco.addAmplifier(new TargetingAmplifier<Node, Link<LinkQuality>, LinkQuality>(1));
	
		manet.addFlow(new Flow<Node, Link<LinkQuality>, LinkQuality>());
			
		manet.initialize();
	}
	
	public void compute() {			
		
		aco.run();	
				
		if(aco.foundSolution())	{	
			
			for(int i=0; i < aco.getSolution().getSolution().getPaths().size(); i++) { 
				
				manet.getFlow(i).update(aco.getSolution().getSolution().getPaths().get(i));
								
				manet.deployFlow(manet.getFlow(i));		
			}
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
			new RandomNumbers());

		generator.generate(properties);
		
		manet.initialize();
					
		/**************************************************************************************************************************************/
		/* Setup & compute */
				
		RandomNumbers random = new RandomNumbers();	
		
		for(int i=0; i < 1; i++)
			manet.addFlow(new Flow<Node, Link<LinkQuality>, LinkQuality>(
				manet.getVertices().get(random.getRandom(0, manet.getVertices().size())), 
				manet.getVertices().get(random.getRandom(0, manet.getVertices().size())), new DataRate((random.getRandom(5000, 5000)))));	
				
		MultiFlowDistribution multiFlowDistribution = new MultiFlowDistribution(manet);		
		multiFlowDistribution.initialize();
		multiFlowDistribution.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */

		VisualGraphApp<Node, Link<LinkQuality>, LinkQuality> visualGraphApp = 
				new VisualGraphApp<Node, Link<LinkQuality>, LinkQuality>(manet, manet.getFlows(), new LinkQualityPrinter());
	}	
}
