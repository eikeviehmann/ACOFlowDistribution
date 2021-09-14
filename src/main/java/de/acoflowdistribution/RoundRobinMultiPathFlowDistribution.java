package de.acoflowdistribution;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import de.aco.alg.ACOProperties;
import de.aco.alg.multipath.RoundRobinMultiPath;
import de.acoflowdistribution.model.CapacityConsumer;
import de.acoflowdistribution.model.FlowContextEvaluator;
import de.acoflowdistribution.model.FlowEvaluator;
import de.acoflowdistribution.model.UtilizationRequirement;
import de.jgraphlib.graph.generator.NetworkGraphGenerator;
import de.jgraphlib.graph.generator.NetworkGraphProperties;
import de.jgraphlib.graph.generator.GraphProperties.DoubleRange;
import de.jgraphlib.graph.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;
import de.manetmodel.evaluator.DoubleScope;
import de.manetmodel.evaluator.ScalarLinkQualityEvaluator;
import de.manetmodel.generator.FlowProblemGenerator;
import de.manetmodel.generator.FlowProblemProperties;
import de.manetmodel.gui.LinkQualityScorePrinter;
import de.manetmodel.gui.LinkUtilizationPrinter;
import de.manetmodel.mobilitymodel.PedestrianMobilityModel;
import de.manetmodel.network.scalar.ScalarLinkQuality;
import de.manetmodel.network.scalar.ScalarRadioFlow;
import de.manetmodel.network.scalar.ScalarRadioLink;
import de.manetmodel.network.scalar.ScalarRadioMANET;
import de.manetmodel.network.scalar.ScalarRadioMANETSupplier;
import de.manetmodel.network.scalar.ScalarRadioModel;
import de.manetmodel.network.scalar.ScalarRadioNode;
import de.manetmodel.units.DataRate;
import de.manetmodel.units.Speed;
import de.manetmodel.units.Unit;
import de.manetmodel.units.Watt;
import de.manetmodel.units.Speed.SpeedRange;

public class RoundRobinMultiPathFlowDistribution {

	//@formatter:off
	
	private RoundRobinMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET> aco;
	private ScalarRadioMANET manet;
		
	public RoundRobinMultiPathFlowDistribution(ScalarRadioMANET manet) {
		this.manet = manet;
		this.manet.initialize();
	}
	
	public void initialize() {	
		
		ACOProperties properties = new ACOProperties();
		properties.antQuantity = 300;
		properties.antReorientationLimit = 10;	
		properties.iterationQuantity = 10;
		
		aco = new RoundRobinMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>(properties);			
		aco.setMetric((ScalarRadioLink link) -> {return (double) link.getUtilization().get();});				
		aco.setAntConsumer(new CapacityConsumer<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());
		aco.setAntRequirement(new UtilizationRequirement<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());	
		//aco.setAntAmplifier(new TargetingAmplifier<_Node, _Link, _LinkQuality>(0.1, 5, 5));		
		aco.setAntEvaluator(new FlowEvaluator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());	
		aco.setAntGroupEvaluator(new FlowContextEvaluator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());	
		aco.initialize(manet);
		
		/*List<myMANET> copies = new ArrayList<myMANET>();
		for(int i=0; i<4; i++)
			copies.add(manet.copy());	
		aco.initialize(4, 4, manet, copies);*/	
	}
	
	public void compute() {			
		
		aco.run();	
				
		if(aco.foundSolution())	{	
			for(int i=0; i < aco.getSolution().getAnts().getPaths().size(); i++) { 	
				manet.getFlow(i).update(aco.getSolution().getAnts().getPaths().get(i));			
				manet.deployFlow(manet.getFlow(i));		
			}
		}			
	}
	
	public static void main(String args[]) throws InvocationTargetException, InterruptedException {
				
		/**************************************************************************************************************************************/
		/* Prepare model */
		
		ScalarRadioModel radioModel = new ScalarRadioModel(
				new Watt(0.002d), 
				new Watt(1e-11), 1000d, 
				2412000000d,
				/* maxCommunicationRange */ 100d);
		
		PedestrianMobilityModel mobilityModel = new PedestrianMobilityModel(
				new RandomNumbers(), 
				new SpeedRange(0, 100, Unit.TimeSteps.second, Unit.Distance.meter), 
				new Speed(50, Unit.Distance.meter, Unit.TimeSteps.second));
		
		ScalarLinkQualityEvaluator evaluator = new ScalarLinkQualityEvaluator(
				new DoubleScope(0d, 1d), radioModel, mobilityModel);
		
		ScalarRadioMANET manet = new ScalarRadioMANET(new ScalarRadioMANETSupplier().getNodeSupplier(),
			new ScalarRadioMANETSupplier().getLinkSupplier(),
			new ScalarRadioMANETSupplier().getLinkPropertySupplier(),
			new ScalarRadioMANETSupplier().getFlowSupplier(),
			radioModel, mobilityModel, evaluator);
							
		NetworkGraphProperties properties = new NetworkGraphProperties(
				/* playground width */ 			1024,
				/* playground height */ 		768, 
				/* number of vertices */ 		new IntRange(100, 150),
				/* distance between vertices */ new DoubleRange(50d, 100d),
				/* edge distance */ 			new DoubleRange(100d, 100d));

		NetworkGraphGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality> generator = 
				new NetworkGraphGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
						manet, 
						new ScalarRadioMANETSupplier().getLinkPropertySupplier(), 
						new RandomNumbers());

		generator.generate(properties);
							
		/**************************************************************************************************************************************/
		/* Setup problem & compute*/
					
		FlowProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> flowProblemGenerator = 
				new FlowProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow>(
						new RandomNumbers(), 
						new ScalarRadioMANETSupplier().getFlowSupplier());
		
		FlowProblemProperties flowProblemProperties = new FlowProblemProperties();
		flowProblemProperties.pathCount = 5;
		flowProblemProperties.minLength = 5;
		flowProblemProperties.maxLength = 10;
		flowProblemProperties.minDemand = new DataRate(100);
		flowProblemProperties.maxDemand = new DataRate(100);
		manet.setPaths(flowProblemGenerator.generate(manet, flowProblemProperties));
				
		RoundRobinMultiPathFlowDistribution multiFlowDistribution = new RoundRobinMultiPathFlowDistribution(manet);		
		multiFlowDistribution.initialize();
		multiFlowDistribution.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */	
		SwingUtilities.invokeAndWait(new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(manet, new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));
	}	
}
