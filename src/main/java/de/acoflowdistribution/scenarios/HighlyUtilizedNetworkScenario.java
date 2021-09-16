package de.acoflowdistribution.scenarios;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import de.jgraphlib.graph.algorithms.DijkstraShortestPath;
import de.jgraphlib.graph.generator.NetworkGraphGenerator;
import de.jgraphlib.graph.generator.NetworkGraphProperties;
import de.jgraphlib.graph.generator.GraphProperties.DoubleRange;
import de.jgraphlib.graph.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.Log;
import de.jgraphlib.util.Log.HeaderLevel;
import de.jgraphlib.util.RandomNumbers;
import de.manetmodel.algorithm.CplexFlowDistribution;
import de.manetmodel.evaluator.DoubleScope;
import de.manetmodel.evaluator.ScalarLinkQualityEvaluator;
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

public class HighlyUtilizedNetworkScenario extends FlowDistributionScenario<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET> {

	public HighlyUtilizedNetworkScenario() {
		
		Log log = new Log();
		RandomNumbers randomNumbers = new RandomNumbers();	
		seed = randomNumbers.getSeed();
		
		/**************************************************************************************************************************************/
		/* (1) Initialize the model */
		
		ScalarRadioModel radioModel = new ScalarRadioModel(
				new Watt(0.002d), 
				new Watt(1e-11), 1000d, 
				2412000000d,
				/* maxCommunicationRange */ 100d);
		
		PedestrianMobilityModel mobilityModel = new PedestrianMobilityModel(
				randomNumbers, 
				new SpeedRange(0, 100, Unit.TimeSteps.second, Unit.Distance.meter), 
				new Speed(50, Unit.Distance.meter, Unit.TimeSteps.second));
			
		network = new ScalarRadioMANET(new ScalarRadioMANETSupplier().getNodeSupplier(),
			new ScalarRadioMANETSupplier().getLinkSupplier(),
			new ScalarRadioMANETSupplier().getLinkPropertySupplier(),
			new ScalarRadioMANETSupplier().getFlowSupplier(),
			radioModel, 
			mobilityModel, 
			new ScalarLinkQualityEvaluator(new DoubleScope(0d, 1d), radioModel, mobilityModel));
				
		/**************************************************************************************************************************************/
		/* (2) Generate the model's graph */		
		
		NetworkGraphProperties properties = new NetworkGraphProperties(
				/* playground width */ 			1024,
				/* playground height */ 		768, 
				/* number of vertices */ 		new IntRange(100, 100),
				/* distance between vertices */ new DoubleRange(50d, 100d),
				/* edge distance */ 			new DoubleRange(100d, 100d));

		NetworkGraphGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality> generator = 
				new NetworkGraphGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
						network, 
						new ScalarRadioMANETSupplier().getLinkPropertySupplier(), 
						randomNumbers);

		generator.generate(properties);	
		
		network.initialize();
		
		/**************************************************************************************************************************************/
		/* (3) Generate source-target pairs of shortest-paths (Dijkstra) that over-utilize actively transmitting links */	
		
		log.infoHeader(HeaderLevel.h2, "(3) Generate source-target pairs");
		
		OverUtilzedProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> overUtilizedProblemGenerator = 
				new OverUtilzedProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow>(
						network, 
						(ScalarLinkQuality w) -> { return w.getScore();});

		OverUtilizedProblemProperties problemProperties = new OverUtilizedProblemProperties();
		problemProperties.pathCount = 5;
		problemProperties.minLength = 10;
		problemProperties.maxLength = 20;
		problemProperties.minDemand = new DataRate(100);
		problemProperties.maxDemand = new DataRate(200);
		problemProperties.overUtilizationPercentage = 5;
		
		sourceTargetPairs = overUtilizedProblemGenerator.compute(problemProperties, randomNumbers);
			
		network.addFlows(sourceTargetPairs);
		
		for(ScalarRadioFlow flow : sourceTargetPairs)
			log.info(String.format("Flow %d: source %d, target %d, dataRate: %s", flow.getID(), flow.getSource().getID(), flow.getTarget().getID(), flow.getDataRate().toString()));
				
		/**************************************************************************************************************************************/
		/* (4) Check if network allows a feasible non-over-utilized (optimal) flow configuration (benchmark) */	
		
		log.infoHeader(HeaderLevel.h2, "(3) Feasible (cplex) deployment");
				
		CplexFlowDistribution<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> cplexFlowDistribution = new 
				CplexFlowDistribution<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow>();
		
		feasibleDistribution = cplexFlowDistribution.generateFeasibleSolution(network);
		
		//optimalDistribution = cplexFlowDistribution.generateOptimalSolution(network);
		
		for(ScalarRadioFlow flow : feasibleDistribution)
			log.info(String.format("Flow %d: %s", flow.getID(), flow.toString()));		
			
		/**************************************************************************************************************************************/
		
		log.info(String.format("Scenario was generated with seed %d ", seed));
	}
	
	public static void main(String args[]) throws InvocationTargetException, InterruptedException {
		
		HighlyUtilizedNetworkScenario scenario = new HighlyUtilizedNetworkScenario();
								
		/**************************************************************************************************************************************/
					
		for(ScalarRadioFlow flow : scenario.network.getFlows()) 
			scenario.network.deployFlow(flow);
		
		SwingUtilities.invokeAndWait(
				new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
						scenario.network, 
						new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));
		
		scenario.network.undeployFlows();
		
		/**************************************************************************************************************************************/
		/* (2) Plot over-utilized (Dijkstra) distribution */	
			
		DijkstraShortestPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality> dijkstraShortestPath = 
				new DijkstraShortestPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(scenario.network);
		
		for(ScalarRadioFlow flow : scenario.sourceTargetPairs) {
			flow.update(dijkstraShortestPath.compute(
					flow.getSource(), 
					flow.getTarget(), 
					(ScalarLinkQuality w) -> { return w.getScore();}));
			scenario.network.deployFlow(flow);
		}
				
		SwingUtilities.invokeAndWait(
				new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
						scenario.network, 
						new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));
	}

}
