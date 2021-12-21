package de.acoflowdistribution.test;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import de.aco.alg.ACOProperties;
import de.aco.alg.multipath.IndependentMultiPath;
import de.aco.pheromone.ScoreOrder;
import de.acoflowdistribution.model.FlowDeploymentEvaluator;
import de.acoflowdistribution.model.LinkCapacityConsumer;
import de.acoflowdistribution.model.UtilizationRequirement;
import de.jgraphlib.generator.ClusterGraphProperties;
import de.jgraphlib.generator.CorridorClusterGraphGenerator;
import de.jgraphlib.generator.CorridorClusterGraphProperties;
import de.jgraphlib.generator.GridGraphGenerator;
import de.jgraphlib.generator.GridGraphProperties;
import de.jgraphlib.generator.RandomClusterGraphGenerator;
import de.jgraphlib.generator.GraphProperties.DoubleRange;
import de.jgraphlib.generator.GraphProperties.IntRange;
import de.jgraphlib.graph.elements.EdgeDistance;
import de.jgraphlib.graph.elements.Position2D;
import de.jgraphlib.graph.elements.Vertex;
import de.jgraphlib.graph.elements.WeightedEdge;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;
import de.manetmodel.evaluator.DoubleScope;
import de.manetmodel.evaluator.ScalarLinkQualityEvaluator;
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
import de.manetmodel.units.DataUnit.Type;
import de.manetmodel.units.Speed.SpeedRange;

public class IndependentMultiPathClusterTest {

	public static void main(String args[]) throws InvocationTargetException, InterruptedException {

		/**************************************************************************************************************************************/
		RandomNumbers randomNumbers = new RandomNumbers(7277246775525279348L);
		
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
			
		ScalarRadioMANET network = new ScalarRadioMANET(new ScalarRadioMANETSupplier().getNodeSupplier(),
			new ScalarRadioMANETSupplier().getLinkSupplier(),
			new ScalarRadioMANETSupplier().getLinkPropertySupplier(),
			new ScalarRadioMANETSupplier().getFlowSupplier(),
			radioModel, 
			mobilityModel, 
			new ScalarLinkQualityEvaluator(new DoubleScope(0d, 1d), radioModel, mobilityModel));

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
		
		SwingUtilities.invokeAndWait(new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
				network, new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));
		
		/**************************************************************************************************************************************/	
		/*ACOProperties acoProperties = new ACOProperties(ScoreOrder.DESCENDING);	
		acoProperties.iterationQuantity = 10;
		acoProperties.antQuantity = 100000;
		acoProperties.antReorientationLimit = 50;

		IndependentMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET> aco = 
				new IndependentMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>(acoProperties);
				
		//network.addPath(new ScalarRadioFlow(network.getVertices().get(1), network.getVertices().get(36), new DataRate(1, Type.kilobit)));
		
		aco.setMetric((ScalarRadioLink link) -> {return link.getWeight().getScore();});
		aco.initialize(network);	
		aco.setAntGroupRequirement(new UtilizationRequirement<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());
		aco.setAntGroupEvaluator(new FlowDeploymentEvaluator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());
		aco.setAntConsumer(new LinkCapacityConsumer<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());
		//aco.run();
		
		if(aco.foundSolution())	{	
			
			for(int i=0; i < aco.getSolution().getAnts().getPaths().size(); i++) {
				network.getPath(i).update(aco.getSolution().getAnts().getPaths().get(i));	
				network.deployFlow(network.getPath(i));
			}
			
			System.out.println(String.format("Overutilization: %d", network.getOverUtilization().get()));
			
			SwingUtilities.invokeAndWait(new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
					network, new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));
		}*/
	}	
	
}
