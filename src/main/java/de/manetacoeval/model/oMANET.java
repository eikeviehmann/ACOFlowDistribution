package de.manetacoeval.model;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import de.aco.alg.ACOGraph;
import de.aco.ant.Ant;
import de.aco.ant.AntGroup;
import de.jgraphlib.graph.elements.Path;
import de.jgraphlib.util.Tuple;
import de.manetmodel.network.Flow;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANET;
import de.manetmodel.network.Node;
import de.manetmodel.network.mobility.MobilityModel;
import de.manetmodel.network.radio.IRadioModel;

public class oMANET extends MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> implements ACOGraph<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>>{

	public oMANET(
			Supplier<Node> vertexSupplier, 
			Supplier<Link<LinkQuality>> edgeSupplier,
			Supplier<LinkQuality> edgeWeightSupplier, 
			Supplier<Flow<Node, Link<LinkQuality>, LinkQuality>> flowSupplier,
			IRadioModel radioModel, MobilityModel mobilityModel) {
		
		super(vertexSupplier, edgeSupplier, edgeWeightSupplier, flowSupplier, radioModel, mobilityModel);
	}
		
	public oMANET(oMANET acomanet) {
		super(acomanet);
	}

	public oMANET copy() {
		return new oMANET(this);
	}

	@Override
	public void consume(Ant<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> ant) {	
				
		//System.out.println(String.format("Increase utilization by %d", getFlows().get(ant.getColonyID()).getDataRate().get()));
		
		increaseUtilizationBy(ant.getPath().getLastEdge(), ant.getPath().getDataRate());								
	}

	@Override
	public boolean require(Ant<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> ant, Tuple<Link<LinkQuality>,Node> linkNodeTuple) {
			
		for(Link<LinkQuality> utilizedLink : getActiveUtilizedLinksOf(linkNodeTuple.getFirst())) {	
			
			System.out.println(String.format("Utilization by %d", utilizedLink.getWeight().getUtilization().get()));
			
			if(utilizedLink.getWeight().getUtilization().get() + getFlows().get(ant.getColonyID()).getDataRate().get() > utilizedLink.getWeight().getTransmissionRate().get())								
				return false;
		}
		
		return true;
	}

	@Override
	public boolean require(Ant<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> ant,
			AntGroup<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> antGroup,
			Tuple<Link<LinkQuality>, Node> nextHop) {
		return false;
	}

	@Override
	public double evaluate(Flow<Node, Link<LinkQuality>, LinkQuality> path, Function<LinkQuality, Double> metric) {
		return 0;
	}

	@Override
	public double evaluate(List<Flow<Node, Link<LinkQuality>, LinkQuality>> path, Function<LinkQuality, Double> metric) {
		return 0;
	}

	@Override
	public void resetResources() {
		// TODO Auto-generated method stub
		
	}

}
