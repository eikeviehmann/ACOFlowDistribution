package de.acoflowdistribution.model;

import java.util.function.Function;

import de.aco.ant.Ant;
import de.aco.ant.evaluation.AntEvaluator;
import de.manetmodel.network.Flow;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANET;
import de.manetmodel.network.Node;

public class FlowEvaluator<N extends Node, L extends Link<W>, W extends LinkQuality, F extends Flow<N,L,W>, M extends MANET<N,L,W,F>> extends AntEvaluator<N, L, W, F, M> {

	@Override
	public double evaluate(M graph, Ant<N, L, W, F> ant, Function<L, Double> metric) {
		
		double scoreSum = 0;
		
		for(L link : ant.getPath().getEdges())
			scoreSum += link.getWeight().getScore();
		
		return scoreSum;
			
	}	
}
