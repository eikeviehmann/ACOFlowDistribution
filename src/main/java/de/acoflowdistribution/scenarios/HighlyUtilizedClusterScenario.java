package de.acoflowdistribution.scenarios;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import de.jgraphlib.generator.ClusterGraphProperties;
import de.jgraphlib.generator.NetworkGraphGenerator;
import de.jgraphlib.generator.NetworkGraphProperties;
import de.jgraphlib.generator.RandomClusterGraphGenerator;
import de.jgraphlib.generator.GraphProperties.DoubleRange;
import de.jgraphlib.generator.GraphProperties.IntRange;
import de.jgraphlib.graph.algorithms.DijkstraShortestPath;
import de.jgraphlib.graph.elements.EdgeDistance;
import de.jgraphlib.graph.elements.Position2D;
import de.jgraphlib.graph.elements.Vertex;
import de.jgraphlib.graph.elements.WeightedEdge;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.Log;
import de.jgraphlib.util.Log.HeaderLevel;
import de.jgraphlib.util.RandomNumbers;
import de.manetmodel.algorithm.CplexFlowDistribution;
import de.manetmodel.evaluator.DoubleScope;
import de.manetmodel.evaluator.ScalarLinkQualityEvaluator;
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

public class HighlyUtilizedClusterScenario extends FlowDistributionScenario<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET> {

	public HighlyUtilizedClusterScenario() {
		
		Log log = new Log();
		RandomNumbers randomNumbers = new RandomNumbers(12345);	
		seed = randomNumbers.getSeed();
		
		/**************************************************************************************************************************************/
		/* (1) Initialize the model */
		
		ScalarRadioModel radioModel = new ScalarRadioModel(
				new Watt(0.002d), 
				new Watt(1e-11), 1000d, 
				2412000000d,
				100d,
				100d);
		
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
			
		ClusterGraphProperties properties = new ClusterGraphProperties(
				/* playground width */ 			2048,
				/* playground height */ 		1024, 
				/* number of vertices */ 		new IntRange(100, 100),
				/* distance between vertices */ new DoubleRange(50d, 50d),
				null, /* edge distance */ 			new DoubleRange(50d, 75d),
				/* corridorQuantity*/ 			5,
				/* corridorEdgeDistance*/ 		new DoubleRange(250d, 300d));

		RandomClusterGraphGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality> generator = 
				new RandomClusterGraphGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
						network, randomNumbers);
		
		generator.generate(properties);
		
		network.initialize();
		
		/**************************************************************************************************************************************/
		/* (3) Generate source-target pairs of shortest-paths (Dijkstra) that over-utilize actively transmitting links */	
		
		log.infoHeader(HeaderLevel.h2, "(3) Generate source-target pairs");
		
		OverUtilzedProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> overUtilizedProblemGenerator = 
				new OverUtilzedProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow>(
						network, 
						(ScalarLinkQuality w) -> { return w.getScore();},
						randomNumbers);
		
		OverUtilizedProblemProperties problemProperties = new OverUtilizedProblemProperties(
				/*pathCount*/ 					10, 
				/*minLength*/ 					10, 
				/*maxLength*/ 					20,
				/*minDemand*/ 					new DataRate(100), 
				/*maxDemand*/ 					new DataRate(100), 
				/*uniqueSourceTarget*/ 			true, 
				/*overUtilizationPercentage*/ 	25, 
				/*increaseFactor*/ 				new DataRate(10));
		
		sourceTargetPairs = overUtilizedProblemGenerator.compute(problemProperties);
			
		network.addFlows(sourceTargetPairs);
				
		/**************************************************************************************************************************************/
		/* (4) Check if network allows a feasible non-over-utilized (optimal) flow configuration (benchmark) */	
		
		/*log.infoHeader(HeaderLevel.h2, "(3) Feasible (cplex) deployment");
				
		CplexFlowDistribution<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> cplexFlowDistribution = new 
				CplexFlowDistribution<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow>();
		
		feasibleDistribution = cplexFlowDistribution.generateFeasibleSolution(network);
		
		//optimalDistribution = cplexFlowDistribution.generateOptimalSolution(network);
		
		for(ScalarRadioFlow flow : feasibleDistribution)
			log.info(String.format("Flow %d: %s", flow.getID(), flow.toString()));	*/	
			
		/**************************************************************************************************************************************/
		
		log.info(String.format("Scenario was generated with seed %d ", seed));
	}
	
	public static void main(String args[]) throws InvocationTargetException, InterruptedException {
		
		HighlyUtilizedClusterScenario scenario = new HighlyUtilizedClusterScenario();
								
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
		
		//System.out.println(scenario.network.toString());
		
		/*SwingUtilities.invokeAndWait(
				new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
						scenario.network, 
						new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));*/
	}

}
