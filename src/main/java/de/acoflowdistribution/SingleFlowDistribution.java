package de.acoflowdistribution;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import de.aco.alg.ACOProperties;
import de.aco.alg.singlepath.SinglePath;
import de.aco.pheromone.ScoreOrder;
import de.acoflowdistribution.model.LinkCapacityConsumer;
import de.jgraphlib.generator.NetworkGraphGenerator;
import de.jgraphlib.generator.NetworkGraphProperties;
import de.jgraphlib.generator.GraphProperties.DoubleRange;
import de.jgraphlib.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;
import de.manetmodel.evaluator.DoubleScope;
import de.manetmodel.evaluator.ScalarLinkQualityEvaluator;
import de.manetmodel.gui.printer.LinkQualityScorePrinter;
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

public class SingleFlowDistribution {

	//@formatter:off

	private SinglePath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET> aco;
	private ScalarRadioMANET manet;	
	
	public SingleFlowDistribution(ScalarRadioMANET manet) {
		this.manet = manet;
		this.manet.initialize();
	}
	
	public void initialize() {
		
		ACOProperties properties = new ACOProperties(ScoreOrder.DESCENDING);
		properties.antQuantity = 1000;
		properties.antReorientationLimit = 1;	
		properties.iterationQuantity = 10;
		
		aco = new SinglePath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>(properties);				
		aco.setMetric((ScalarRadioLink link) -> {return (double) link.getNumberOfUtilizedLinks();});			
		aco.setAntConsumer(new LinkCapacityConsumer<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>());
		//aco.initialize(8, 8, manet, Collections.nCopies(8, manet.copy()));	
		aco.initialize(manet);
	}
	
	public void compute() {			
		
		aco.run();	
				
		if(aco.foundSolution()) {
			manet.getFlow(0).update(aco.getSolution().getAnts().getPath());
			manet.deployFlow(manet.getFlow(0));	
		}	
	}
	
	public static void main(String args[]) throws InvocationTargetException, InterruptedException {
					
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
			radioModel, 
			mobilityModel,
			evaluator);
							
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
							
		/**************************************************************************************************************************************/
		/* Setup & compute */
						
		manet.addFlow(new ScalarRadioFlow(
				manet.getVertices().get(new RandomNumbers().getRandom(0, manet.getVertices().size())),
				manet.getVertices().get(new RandomNumbers().getRandom(0, manet.getVertices().size())),
				new DataRate(100)));
				
		SingleFlowDistribution flowDistribution = new SingleFlowDistribution(manet);		
		flowDistribution.initialize();
		flowDistribution.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */	
		SwingUtilities.invokeAndWait(new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(manet, new LinkQualityScorePrinter<ScalarLinkQuality>()));	
		//SwingUtilities.invokeAndWait(new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(manet, new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));		

	}
}
