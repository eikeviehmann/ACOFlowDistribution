package de.acomanetopt.manetmodel;

import java.util.HashSet;
import java.util.Set;

import de.jgraphlib.graph.WeightedEdge;

public class Link<W extends LinkQuality> extends WeightedEdge<W> {

	private Set<Integer> utilizedLinkIds;
	private boolean isActive;

	public Link() {
		utilizedLinkIds = new HashSet<Integer>();
		isActive = false;
	}

	public Set<Integer> getUtilizedLinkIds() {
		return utilizedLinkIds;
	}

	public void setUtilizedLinks(Set<Integer> l) {
		utilizedLinkIds.addAll(l);
	}

	@Override
	public String toString() {
		return new StringBuffer("ID: ").append(getID()).toString();
	}

	public void setIsActive(boolean isParticipant) {
		this.isActive = isParticipant;

	}

	public boolean getIsActive() {
		return this.isActive;
	}
}
