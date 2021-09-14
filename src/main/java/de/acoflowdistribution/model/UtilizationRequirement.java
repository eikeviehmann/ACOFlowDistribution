package de.acoflowdistribution.model;

import de.aco.ant.Ant;
import de.aco.ant.AntRequirement;
import de.manetmodel.network.Flow;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANET;
import de.manetmodel.network.Node;

public class UtilizationRequirement<N extends Node, L extends Link<W>, W extends LinkQuality, F extends Flow<N,L,W>, M extends MANET<N,L,W,F>> extends AntRequirement<N, L, W, F, M> {

	@Override
	public boolean check(M manet, Ant<N, L, W, F> ant, L link) {
					
		for (Link<W> activeUtilizedLink : manet.getActiveUtilizedLinksOf(link)) 
			if (activeUtilizedLink.getUtilization().get() + ant.getPath().getDataRate().get() > activeUtilizedLink.getTransmissionRate().get())
				return false;
			
		return true;
	}
}
