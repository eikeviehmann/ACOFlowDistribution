package de.acoflowdistribution;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import de.aco.alg.ACOProperties;
import de.aco.alg.multipath.IndependentMultiPath;
import de.acoflowdistribution.model.CapacityConsumer;
import de.acoflowdistribution.model.FlowContextEvaluator;
import de.acoflowdistribution.model.FlowEvaluator;
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
import de.manetmodel.generator.OverUtilizedProblemProperties;
import de.manetmodel.generator.OverUtilzedProblemGenerator;
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

public class IndependentMultiFlowDistribution {

	//@formatter:off
	
	private IndependentMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET> aco;
	private ScalarRadioMANET manet;
		
	public IndependentMultiFlowDistribution(ScalarRadioMANET manet) {
		this.manet = manet;
	}
	
	public void initialize() {	
		
		ACOProperties properties = new ACOProperties();
		properties.antQuantity = 1000;
		properties.antReorientationLimit = 25;	
		properties.iterationQuantity = 10;
		
		aco = new IndependentMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>(properties);			
		aco.setMetric((ScalarRadioLink link) -> {return (double) link.getUtilization().get();});				
		aco.setAntConsumer(new CapacityConsumer<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());
		aco.setAntEvaluator(new FlowEvaluator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());	
		aco.setAntGroupEvaluator(new FlowContextEvaluator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());	
		aco.initialize(manet);
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
				/* number of vertices */ 		new IntRange(100, 100),
				/* distance between vertices */ new DoubleRange(50d, 100d),
				/* edge distance */ 			new DoubleRange(100d, 100d));

		NetworkGraphGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality> generator = 
				new NetworkGraphGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
						manet, 
						new ScalarRadioMANETSupplier().getLinkPropertySupplier(), 
						new RandomNumbers());

		generator.generate(properties);
		
		manet.initialize();
							
		/**************************************************************************************************************************************/
		/* Setup problem & compute*/
					
		FlowProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> flowProblemGenerator = 
				new FlowProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow>(
						new RandomNumbers(), 
						new ScalarRadioMANETSupplier().getFlowSupplier());
		
		FlowProblemProperties flowProblemProperties = new FlowProblemProperties();
		flowProblemProperties.pathCount = 10;
		flowProblemProperties.minLength = 5;
		flowProblemProperties.maxLength = 10;
		flowProblemProperties.minDemand = new DataRate(100);
		flowProblemProperties.maxDemand = new DataRate(100);
		manet.setPaths(flowProblemGenerator.generate(manet, flowProblemProperties));
		
		/*OverUtilzedProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> overUtilizedProblemGenerator = 
				new OverUtilzedProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow>(
						manet, 
						(ScalarLinkQuality w) -> { return w.getScore();});

		OverUtilizedProblemProperties problemProperties = new OverUtilizedProblemProperties();
		problemProperties.pathCount = 10;
		problemProperties.minLength = 10;
		problemProperties.maxLength = 20;
		problemProperties.minDemand = new DataRate(100);
		problemProperties.maxDemand = new DataRate(100);
		problemProperties.overUtilizationPercentage = 1;
		
		manet.addFlows(overUtilizedProblemGenerator.compute(problemProperties, new RandomNumbers()));*/
				
		IndependentMultiFlowDistribution multiFlowDistribution = new IndependentMultiFlowDistribution(manet);		
		multiFlowDistribution.initialize();
		multiFlowDistribution.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */	
		SwingUtilities.invokeAndWait(new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(manet, new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));
	}
}
