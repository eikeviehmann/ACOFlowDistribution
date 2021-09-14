package de.acoflowdistribution.model;

import java.util.function.Function;

import de.aco.ant.Ant;
import de.aco.ant.AntGroup;
import de.aco.ant.evaluation.AntGroupEvaluator;
import de.manetmodel.network.Flow;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANET;
import de.manetmodel.network.Node;

public class FlowContextEvaluator <N extends Node, L extends Link<W>, W extends LinkQuality, F extends Flow<N,L,W>, M extends MANET<N,L,W,F>> extends AntGroupEvaluator <N, L, W, F, M> {

	@Override
	public double evaluate(M graph, AntGroup<N, L, W, F> antGroup, Function<L, Double> metric) {
		double costs = 0;
		for (Ant<N, L, W, F> ant : antGroup)
			costs += ant.getPath().size() * ant.getPath().getDataRate().get();
		return costs;
	}
}
