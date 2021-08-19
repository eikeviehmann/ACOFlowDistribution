package de.manetacoeval.model;

import de.aco.ant.Ant;
import de.aco.ant.extensions.AntRequirement;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.Node;
import de.manetmodel.network.myFlow;
import de.manetmodel.network.myMANET;

public class UtilizationRequirement extends AntRequirement<Node, Link<LinkQuality>, LinkQuality, myFlow, myMANET> {

	@Override
	public boolean check(myMANET manet, Ant<Node, Link<LinkQuality>, LinkQuality, myFlow> ant, Link<LinkQuality> edge) {
					
		for (Link<LinkQuality> link : manet.getActiveUtilizedLinksOf(edge)) 
			if (link.getWeight().getUtilization().get() + ant.getPath().getDataRate().get() > link.getWeight().getTransmissionRate().get())
				return false;
			
		return true;
	}

}
