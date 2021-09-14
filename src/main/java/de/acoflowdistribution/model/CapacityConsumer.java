package de.acoflowdistribution.model;

import de.aco.ant.Ant;
import de.aco.ant.AntConsumer;
import de.manetmodel.network.Flow;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANET;
import de.manetmodel.network.Node;

public class CapacityConsumer<N extends Node, L extends Link<W>, W extends LinkQuality, F extends Flow<N,L,W>, M extends MANET<N,L,W,F>> extends AntConsumer<N, L, W, F, M> {

	@Override
	public void consume(M manet, Ant<N, L, W, F> ant) {
		manet.increaseUtilizationBy(ant.getPath().getLastEdge(), ant.getPath().getDataRate());				
	}

	@Override
	public void reset(M manet) {
		manet.undeployFlows();	
	}	
}
