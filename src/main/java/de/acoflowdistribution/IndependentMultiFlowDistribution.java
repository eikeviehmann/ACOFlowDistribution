package de.acoflowdistribution;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;

import javax.swing.SwingUtilities;

import de.aco.alg.ACOProperties;
import de.aco.alg.multipath.IndependentMultiPath;
import de.aco.ant.Ant;
import de.aco.ant.AntConsumer;
import de.aco.ant.AntGroup;
import de.aco.ant.evaluation.AntEvaluator;
import de.aco.ant.evaluation.AntGroupEvaluator;
import de.aco.pheromone.ScoreOrder;
import de.jgraphlib.generator.NetworkGraphGenerator;
import de.jgraphlib.generator.NetworkGraphProperties;
import de.jgraphlib.generator.GraphProperties.DoubleRange;
import de.jgraphlib.generator.GraphProperties.IntRange;
import de.jgraphlib.gui.VisualGraphApp;
import de.jgraphlib.util.RandomNumbers;
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

public class IndependentMultiFlowDistribution {

	//@formatter:off
	
	public IndependentMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET> aco;
	public ScalarRadioMANET manet;
		
	public IndependentMultiFlowDistribution(ScalarRadioMANET manet) {
		this.manet = manet;
	}
	
	public void initialize() {	
		
		ACOProperties properties = new ACOProperties(ScoreOrder.DESCENDING);
		properties.antQuantity = 10000;
		properties.antReorientationLimit = 50;	
		properties.iterationQuantity = 10;
		
		aco = new IndependentMultiPath<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>(properties);	
		
		aco.setMetric((ScalarRadioLink link) -> {return (double) link.getWeight().getScore();});	
		
		aco.setAntConsumer(new AntConsumer<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>() {
			@Override
			public void consume(ScalarRadioMANET graph,
					Ant<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> ant) {		
				for(ScalarRadioLink link : ant.getPath().getEdges())
					graph.increaseUtilizationBy(link, ant.getPath().getDataRate());			
			}
			@Override
			public void reset(ScalarRadioMANET graph) {
				graph.undeployFlows();			
			}
		});
		
		aco.setAntEvaluator(new AntEvaluator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>() {
			@Override
			public double evaluate(
					ScalarRadioMANET graph,
					Ant<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> ant,
					Function<ScalarRadioLink, Double> metric) {
				
				double score = 0d;
				for(ScalarRadioLink link : ant.getPath().getEdges())
					score += link.getWeight().getScore();
				return score;
			}
		});
		
		aco.setAntGroupEvaluator(new AntGroupEvaluator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow, ScalarRadioMANET>() {
			@Override
			public double evaluate(ScalarRadioMANET graph,
					AntGroup<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> antGroup,
					Function<ScalarRadioLink, Double> metric) {

				double score = 0d;
				
				for(Ant<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> ant : antGroup) 						
					for(ScalarRadioLink link : ant.getPath().getEdges())
						score += link.getWeight().getScore();
					
				List<ScalarRadioLink> overutilizedLinks = graph.getOverUtilizedLinks();		
				if(overutilizedLinks.size() > 0) 		
					for(ScalarRadioLink link : overutilizedLinks)
						score += link.getOverUtilization().get() * 10;
									
				return score;
			}
		});
		
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
				100d,
				100d);
		
		PedestrianMobilityModel mobilityModel = new PedestrianMobilityModel(
				new RandomNumbers(), 
				new SpeedRange(0, 100, Unit.TimeSteps.second, Unit.Distance.meter), 
				new Speed(50, Unit.Distance.meter, Unit.TimeSteps.second));
		
		ScalarLinkQualityEvaluator evaluator = new ScalarLinkQualityEvaluator(
				new DoubleScope(0d, 10d), de.manetmodel.evaluator.ScoreOrder.DESCENDING, radioModel, mobilityModel);
		
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
					
		OverUtilzedProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> overUtilizedProblemGenerator = 
				new OverUtilzedProblemGenerator<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow>(
						manet, 
						(ScalarLinkQuality w) -> { return w.getScore();});

		OverUtilizedProblemProperties problemProperties = 
				new OverUtilizedProblemProperties(
						10, 10, 20, new DataRate(10), new DataRate(100), false, 5, new DataRate(10));

		
		manet.addFlows(overUtilizedProblemGenerator.compute(problemProperties));
				
		IndependentMultiFlowDistribution multiFlowDistribution = new IndependentMultiFlowDistribution(manet);		
		multiFlowDistribution.initialize();
		multiFlowDistribution.compute();		
		
		/**************************************************************************************************************************************/
		/* Plot graph & solution */	
		SwingUtilities.invokeAndWait(new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(manet, new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));
	}
}
