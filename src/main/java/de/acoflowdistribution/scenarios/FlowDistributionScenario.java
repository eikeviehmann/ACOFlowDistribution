package de.acoflowdistribution.scenarios;

import java.util.List;

import de.manetmodel.mobilitymodel.MobilityModel;
import de.manetmodel.network.Flow;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANET;
import de.manetmodel.network.Node;
import de.manetmodel.radiomodel.RadioModel;

public abstract class FlowDistributionScenario<N extends Node, L extends Link<W>, W extends LinkQuality, F extends Flow<N,L,W>, M extends MANET<N,L,W,F>> {

	public long seed;
	
	public MANET<N,L,W,F> network;
	
	public List<F> sourceTargetPairs;
	
	public List<F> feasibleDistribution;
	
	public List<F> optimalDistribution;
	
	public RadioModel<N, L, W> radioModel;
	
	public MobilityModel mobilityModel;
}
