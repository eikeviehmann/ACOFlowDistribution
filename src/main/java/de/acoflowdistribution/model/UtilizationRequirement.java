package de.acoflowdistribution.model;

import de.aco.ant.Ant;
import de.aco.ant.AntGroup;
import de.aco.ant.AntGroupRequirement;
import de.manetmodel.network.Flow;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANET;
import de.manetmodel.network.Node;

public class UtilizationRequirement<N extends Node, L extends Link<W>, W extends LinkQuality, F extends Flow<N,L,W>, M extends MANET<N,L,W,F>> extends AntGroupRequirement<N, L, W, F, M> {

	@Override
	public boolean check(M graph, AntGroup<N, L, W, F> antGroup) {
		
		for(Ant<N, L, W, F> ant : antGroup) 
			for(L link : ant.getPath().getEdges())
				if(link.isOverutilized()) return false;
			
		return true;			
	}
}
