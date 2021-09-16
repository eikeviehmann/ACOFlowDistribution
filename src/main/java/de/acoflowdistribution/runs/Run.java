package de.acoflowdistribution.runs;

import de.acoflowdistribution.RoundRobinMultiFlowDistribution;
import de.acoflowdistribution.scenarios.HighlyUtilizedNetworkScenario;
import de.manetmodel.network.scalar.ScalarRadioMANET;

public class Run {
	
	public static void main(String args[]) {
		
		HighlyUtilizedNetworkScenario scenario = 
				new HighlyUtilizedNetworkScenario();
		
		RoundRobinMultiFlowDistribution multiFlowDistribution = 
				new RoundRobinMultiFlowDistribution((ScalarRadioMANET) scenario.network);	
		
		multiFlowDistribution.initialize();
		
		multiFlowDistribution.compute();
	}
}

