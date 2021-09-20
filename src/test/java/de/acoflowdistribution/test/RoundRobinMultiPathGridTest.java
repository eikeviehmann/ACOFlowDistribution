package de.acoflowdistribution.test;

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import de.aco.alg.ACOProperties;
import de.aco.alg.multipath.RoundRobinMultiPath;
import de.aco.pheromone.ScoreOrder;
import de.acoflowdistribution.model.FlowDeploymentEvaluator;
import de.acoflowdistribution.model.LinkCapacityConsumer;
import de.acoflowdistribution.model.UtilizationRequirement;
import de.jgraphlib.graph.generator.GridGraphGenerator;
import de.jgraphlib.graph.generator.GridGraphProperties;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;
import de.manetmodel.evaluator.DoubleScope;
import de.manetmodel.evaluator.ScalarLinkQualityEvaluator;
import de.manetmodel.mobilitymodel.PedestrianMobilityModel;
import de.manetmodel.network.scalar.ScalarLinkQuality;
import de.manetmodel.network.scalar.ScalarRadioFlow;
import de.manetmodel.network.scalar.ScalarRadioLink;
import de.manetmodel.network.scalar.ScalarRadioMANET;
import de.manetmodel.network.scalar.ScalarRadioMANETSupplier;
import de.manetmodel.network.scalar.ScalarRadioModel;
import de.manetmodel.network.scalar.ScalarRadioNode;
import de.manetmodel.gui.*;
import de.manetmodel.units.DataRate;
import de.manetmodel.units.DataUnit.Type;
import de.manetmodel.units.Speed;
import de.manetmodel.units.Unit;
import de.manetmodel.units.Watt;
import de.manetmodel.units.Speed.SpeedRange;

public class RoundRobinMultiPathGridTest {

	public static void main(String args[]) throws InvocationTargetException, InterruptedException {

		/**************************************************************************************************************************************/
		ScalarRadioModel radioModel = new ScalarRadioModel(
				new Watt(0.002d), 
				new Watt(1e-11), 1000d, 
				2412000000d,
				/* maxCommunicationRange */ 100d);
		
		PedestrianMobilityModel mobilityModel = new PedestrianMobilityModel(
				new RandomNumbers(), 
				new SpeedRange(0, 100, Unit.TimeSteps.second, Unit.Distance.meter), 
				new Speed(50, Unit.Distance.meter, Unit.TimeSteps.second));
			
		ScalarRadioMANET network = new ScalarRadioMANET(new ScalarRadioMANETSupplier().getNodeSupplier(),
			new ScalarRadioMANETSupplier().getLinkSupplier(),
			new ScalarRadioMANETSupplier().getLinkPropertySupplier(),
			new ScalarRadioMANETSupplier().getFlowSupplier(),
			radioModel, 
			mobilityModel, 
			new ScalarLinkQualityEvaluator(new DoubleScope(0d, 1d), radioModel, mobilityModel));

		GridGraphProperties properties = new GridGraphProperties(
				/* playground width */ 			500,
				/* playground height */ 		600, 
				/* distance between vertices */ 100, 
				/* length of edges */ 			100);

		GridGraphGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality> generator = 
				new GridGraphGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
						network, new RandomNumbers());
		
		generator.generate(properties);
		
		network.initialize();
		
		/**************************************************************************************************************************************/	
		ACOProperties acoProperties = new ACOProperties(ScoreOrder.DESCENDING);	
		acoProperties.iterationQuantity = 10;
		acoProperties.antQuantity = 100000;
		acoProperties.antReorientationLimit = 50;

		RoundRobinMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET> aco = 
				new RoundRobinMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>(acoProperties);
				
		network.addPath(new ScalarRadioFlow(network.getVertices().get(1), network.getVertices().get(36), new DataRate(0.75, Type.kilobit)));
		network.addPath(new ScalarRadioFlow(network.getVertices().get(3), network.getVertices().get(38), new DataRate(0.75, Type.kilobit)));
		//network.addPath(new ScalarRadioFlow(network.getVertices().get(5), network.getVertices().get(40), new DataRate(0.9, Type.kilobit)));
		
		aco.setMetric((ScalarRadioLink link) -> {return link.getWeight().getScore();});
		aco.initialize(network);	
		aco.setAntGroupRequirement(new UtilizationRequirement<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());
		aco.setAntGroupEvaluator(new FlowDeploymentEvaluator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());
		aco.setAntConsumer(new LinkCapacityConsumer<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());
		aco.run();
		
		if(aco.foundSolution())	{	
			for(int i=0; i < aco.getSolution().getAnts().getPaths().size(); i++) {
				network.getPath(i).update(aco.getSolution().getAnts().getPaths().get(i));	
				network.deployFlow(network.getPath(i));
			}
			
			System.out.println(String.format("Overutilization: %d", network.getOverUtilization().get()));
			
			SwingUtilities.invokeAndWait(new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
					network, new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));
		}
	}	
}