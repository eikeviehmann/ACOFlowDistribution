package de.manetacoeval.model;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import de.aco.ACOGraph;
import de.aco.Ant;
import de.jgraphlib.graph.elements.Path;
import de.manetmodel.network.Flow;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANET;
import de.manetmodel.network.Node;
import de.manetmodel.network.mobility.MobilityModel;
import de.manetmodel.network.radio.IRadioModel;

public class oMANET extends MANET<Node, Link<LinkQuality>, LinkQuality, Flow<Node, Link<LinkQuality>, LinkQuality>> implements ACOGraph<Node, Link<LinkQuality>, LinkQuality>{

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
	public void consume(Ant<Node, Link<LinkQuality>, LinkQuality> ant) {	
				
		//System.out.println(String.format("Increase utilization by %d", getFlows().get(ant.getColonyID()).getDataRate().get()));
		
		increaseUtilizationBy(ant.getPath().getLastEdge(), getFlows().get(ant.getColonyID()).getDataRate());								
	}

	@Override
	public boolean require(Ant<Node, Link<LinkQuality>, LinkQuality> ant, Link<LinkQuality> edge, Node vertex) {
			
		for(Link<LinkQuality> utilizedLink : getActiveUtilizedLinksOf(edge)) {	
			
			System.out.println(String.format("Utilization by %d", utilizedLink.getWeight().getUtilization().get()));
			
			if(utilizedLink.getWeight().getUtilization().get() + getFlows().get(ant.getColonyID()).getDataRate().get() > utilizedLink.getWeight().getTransmissionRate().get())						
				
				return false;
		}
		
		return true;
	}

	@Override
	public double evaluate(Path<Node, Link<LinkQuality>, LinkQuality> path, Function<LinkQuality, Double> metric) {
		
		return path.getCost(metric);
		
	}

	@Override
	public double evaluate(List<Path<Node, Link<LinkQuality>, LinkQuality>> path, Function<LinkQuality, Double> metric) {
		return 0;
	}
	
	@Override
	public void resetResources() {		
		
		eraseFlows();
	
	}
}
