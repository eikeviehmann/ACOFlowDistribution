package de.acomanetopt.manetmodel;

import java.util.HashSet;
import java.util.Set;

import de.jgraphlib.graph.Position2D;
import de.jgraphlib.graph.Vertex;

public class Node extends Vertex<Position2D> {

    private Set<Link<?>> interferredLinks;

    public Node() {
	interferredLinks = new HashSet<Link<?>>();
    }

    public Node(double x, double y) {
	this();
	super.setPosition(new Position2D(x, y));
    }

    public void setInterferedLink(Link<?> l) {
	interferredLinks.add(l);
    }

    public Set<Link<?>> getInterferedLinks() {
	return interferredLinks;
    }

    @Override
    public String toString() {
	return new StringBuffer().append("ID: ").append(this.getID()).append(", #iLinks: ")
		.append(interferredLinks.size()).toString();
    }

}
