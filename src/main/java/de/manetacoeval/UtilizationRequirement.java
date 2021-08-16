package de.manetacoeval;

import de.aco.ant.Ant;
import de.aco.ant.extensions.AntRequirement;
import de.jgraphlib.util.Tuple;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.Node;
import de.manetmodel.network.myFlow;
import de.manetmodel.network.myMANET;

public class UtilizationRequirement extends AntRequirement<Node, Link<LinkQuality>, LinkQuality, myFlow, myMANET>{

	@Override
	public boolean check(myMANET graph, Ant<Node, Link<LinkQuality>, LinkQuality, myFlow> ant, Tuple<Link<LinkQuality>, Node> target) {

		for(Link<LinkQuality> link : graph.getActiveUtilizedLinksOf(ant.getPath().getLastEdge()))			
			if(link.getWeight().getUtilization().get() + ant.getPath().getDataRate().get() > link.getWeight().getTransmissionRate().get())
				return false;
				
		return true;
	}

}
