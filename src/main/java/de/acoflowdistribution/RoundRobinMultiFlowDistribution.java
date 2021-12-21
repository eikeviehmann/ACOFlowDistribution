package de.acoflowdistribution;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import de.aco.alg.ACOProperties;
import de.aco.alg.multipath.RoundRobinMultiPath;
import de.aco.pheromone.ScoreOrder;
import de.acoflowdistribution.model.FlowDeploymentEvaluator;
import de.acoflowdistribution.model.FlowEvaluator;
import de.acoflowdistribution.model.LinkCapacityConsumer;
import de.acoflowdistribution.model.UtilizationRequirement;
import de.jgraphlib.generator.NetworkGraphGenerator;
import de.jgraphlib.generator.NetworkGraphProperties;
import de.jgraphlib.generator.GraphProperties.DoubleRange;
import de.jgraphlib.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;
import de.manetmodel.evaluator.DoubleScope;
import de.manetmodel.evaluator.ScalarLinkQualityEvaluator;
import de.manetmodel.generator.FlowProblemGenerator;
import de.manetmodel.generator.FlowProblemProperties;
import de.manetmodel.generator.OverUtilizedProblemProperties;
import de.manetmodel.generator.OverUtilzedProblemGenerator;
import de.manetmodel.gui.printer.LinkUtilizationPrinter;
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

public class RoundRobinMultiFlowDistribution {

	//@formatter:off
	
	private RoundRobinMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET> aco;
	private ScalarRadioMANET manet;
		
	public RoundRobinMultiFlowDistribution(ScalarRadioMANET manet) {
		this.manet = manet;
	}
	
	public void initialize() {	
		
		ACOProperties properties = new ACOProperties(ScoreOrder.DESCENDING);
		properties.antQuantity = 1000;
		properties.antReorientationLimit = 50;	
		properties.iterationQuantity = 2;
		
		aco = new RoundRobinMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>(properties);			
		aco.setMetric((ScalarRadioLink link) -> {return (double) link.getUtilization().get();});				
		aco.setAntConsumer(new LinkCapacityConsumer<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());
		aco.setAntGroupRequirement(new UtilizationRequirement<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());	
		aco.setAntEvaluator(new FlowEvaluator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());	
		aco.setAntGroupEvaluator(new FlowDeploymentEvaluator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());	
		//aco.initialize(manet);
		aco.initialize(2, 2, manet, manet.nCopies(2));	
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
				100d,
				100d);
		
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
							
		/*OverUtilzedProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> overUtilizedProblemGenerator = 
				new OverUtilzedProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow>(
						manet, 
						(ScalarLinkQuality w) -> { return w.getScore();});

		OverUtilizedProblemProperties problemProperties = new OverUtilizedProblemProperties();
		problemProperties.pathCount = 2;
		problemProperties.minLength = 10;
		problemProperties.maxLength = 20;
		problemProperties.minDemand = new DataRate(100);
		problemProperties.maxDemand = new DataRate(100);
		problemProperties.overUtilizationPercentage = 5;
		
		manet.addFlows(overUtilizedProblemGenerator.compute(problemProperties, new RandomNumbers()));*/
		
		FlowProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> flowProblemGenerator = 
				new FlowProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow>(
						new RandomNumbers(), 
						new ScalarRadioMANETSupplier().getFlowSupplier());
		
		FlowProblemProperties flowProblemProperties = new FlowProblemProperties(
				/*paths*/				2,
				/*minLength*/			5,
				/*maxLength*/			10,
				/*minDemand*/			new DataRate(100),
				/*maxDemand*/			new DataRate(100),
				/*uniqueSourceTarget*/	true);
		
		manet.setPaths(flowProblemGenerator.generate(manet, flowProblemProperties));
				
		RoundRobinMultiFlowDistribution multiFlowDistribution = new RoundRobinMultiFlowDistribution(manet);		
		multiFlowDistribution.initialize();
		multiFlowDistribution.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */	
		SwingUtilities.invokeAndWait(new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(manet, new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));
	}	
}
