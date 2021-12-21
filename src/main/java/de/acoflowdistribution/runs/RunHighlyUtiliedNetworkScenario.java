package de.acoflowdistribution.runs;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import de.acoflowdistribution.IndependentMultiFlowDistribution;
import de.acoflowdistribution.scenarios.HighlyUtilizedClusterScenario;
import de.acoflowdistribution.scenarios.HighlyUtilizedNetworkScenario;
import de.jgraphlib.gui.VisualGraphApp;
import de.manetmodel.algorithm.CplexFlowDistribution;
import de.manetmodel.gui.printer.LinkUtilizationPrinter;
import de.manetmodel.network.scalar.ScalarLinkQuality;
import de.manetmodel.network.scalar.ScalarRadioFlow;
import de.manetmodel.network.scalar.ScalarRadioLink;
import de.manetmodel.network.scalar.ScalarRadioMANET;
import de.manetmodel.network.scalar.ScalarRadioNode;

public class RunHighlyUtiliedNetworkScenario {
	
	public static void main(String args[]) throws InvocationTargetException, InterruptedException {

			
		/**************************************************************************************************************************************/
		/* (1) Cplex */	
		
		/*HighlyUtilizedNetworkScenario cplexScenarioInstance = new HighlyUtilizedNetworkScenario();
		
		CplexFlowDistribution<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow> cplexFlowDistribution = 
				new CplexFlowDistribution<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality, ScalarRadioFlow>();
		
		cplexScenarioInstance.feasibleDistribution = cplexFlowDistribution.generateFeasibleSolution(cplexScenarioInstance.network);
		
		SwingUtilities.invokeAndWait(
				new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
						cplexScenarioInstance.network, 
						new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));*/
		
		/**************************************************************************************************************************************/
		/* (2) ACO */	
		
		HighlyUtilizedNetworkScenario acoScenarioInstance = new HighlyUtilizedNetworkScenario();
		
		IndependentMultiFlowDistribution multiFlowDistribution = 
				new IndependentMultiFlowDistribution((ScalarRadioMANET) acoScenarioInstance.network);	
		
		multiFlowDistribution.initialize();	
		multiFlowDistribution.compute();	
		
		SwingUtilities.invokeAndWait(
				new VisualGraphApp<ScalarRadioNode, ScalarRadioLink, ScalarLinkQuality>(
						acoScenarioInstance.network, 
						new LinkUtilizationPrinter<ScalarRadioLink, ScalarLinkQuality>()));
		
		//System.out.println(String.format("Cplex Solution Quality: %.2f", cplexScenarioInstance.computeScore(cplexScenarioInstance.feasibleDistribution)));		
	}
}

