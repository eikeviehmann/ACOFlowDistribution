package de.acoflowdistribution.test;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import javax.swing.SwingUtilities;
import de.aco.alg.ACOProperties;
import de.aco.alg.multipath.IndependentMultiPath;
import de.aco.ant.Ant;
import de.aco.ant.AntConsumer;
import de.aco.ant.AntGroup;
import de.aco.ant.evaluation.AntGroupEvaluator;
import de.aco.pheromone.ScoreOrder;
import de.jgraphlib.generator.GridGraphGenerator;
import de.jgraphlib.generator.GridGraphProperties;
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
import de.manetmodel.gui.printer.LinkUtilizationPrinter;
import de.manetmodel.units.DataRate;
import de.manetmodel.units.DataUnit.Type;
import de.manetmodel.units.Speed;
import de.manetmodel.units.Unit;
import de.manetmodel.units.Watt;
import de.manetmodel.units.Speed.SpeedRange;

public class IndependentMultiPathGridTest {

	public static void main(String args[]) throws InvocationTargetException, InterruptedException {

		/**************************************************************************************************************************************/
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
		acoProperties.iterationQuantity = 20;
		acoProperties.antQuantity = 10000;
		acoProperties.antReorientationLimit = 50;

		IndependentMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET> aco = 
				new IndependentMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>(acoProperties);
						
		aco.setMetric((ScalarRadioLink link) -> {return link.getWeight().getScore();});
			
		aco.setAntConsumer(new AntConsumer<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>() {
			@Override
			public void consume(ScalarRadioMANET manet, Ant<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> ant) {
				for(ScalarRadioLink link : ant.getPath().getEdges())
					manet.increaseUtilizationBy(link, ant.getPath().getDataRate());
			}
			@Override
			public void reset(ScalarRadioMANET manet) {
				manet.undeployFlows();
			}
		});
		
		aco.setAntGroupEvaluator(new AntGroupEvaluator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>() {
			@Override
			public double evaluate(
					ScalarRadioMANET graph, AntGroup<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> antGroup,
					Function<ScalarRadioLink, Double> metric) {
				double score = 0;			
				for(Ant<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> ant : antGroup) 						
					for(ScalarRadioLink link : ant.getPath().getEdges())
						score += link.getWeight().getScore();					
				if(network.isOverutilized()) 
					score += graph.getOverUtilization().get() * 10;				
				return score;
			}
		});	
						
		network.addPath(new ScalarRadioFlow(network.getVertices().get(6), network.getVertices().get(41), new DataRate(0.75, Type.kilobit)));
		network.addPath(new ScalarRadioFlow(network.getVertices().get(4), network.getVertices().get(39), new DataRate(0.75, Type.kilobit)));
		network.addPath(new ScalarRadioFlow(network.getVertices().get(2), network.getVertices().get(37), new DataRate(0.75, Type.kilobit)));
		network.addPath(new ScalarRadioFlow(network.getVertices().get(0), network.getVertices().get(35), new DataRate(0.75, Type.kilobit)));
		
		aco.initialize(network);
		
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