package de.manetacoeval.model;

import de.aco.ant.Ant;
import de.aco.ant.extensions.AntConsumer;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.Node;
import de.manetmodel.network.myFlow;
import de.manetmodel.network.myMANET;

public class CapacityConsumer extends AntConsumer<Node, Link<LinkQuality>, LinkQuality, myFlow, myMANET>{

	@Override
	public void consume(myMANET manet, Ant<Node, Link<LinkQuality>, LinkQuality, myFlow> ant) {
						
		manet.increaseUtilizationBy(ant.getPath().getLastEdge(), ant.getPath().getDataRate());
		
	}

	@Override
	public void reset(myMANET manet) {
		
		manet.eraseFlows();
		
	}	
}
