package de.acomanetopt.manetmodel;

import java.util.function.Supplier;

import de.jgraphlib.graph.EdgeWeightSupplier;

public class ManetSupplier {

	public NodeSupplier getNodeSupplier() {
		return new NodeSupplier();
	}

	public LinkSupplier getLinkSupplier() {
		return new LinkSupplier();
	}

	public LinkQualitySupplier getLinkQualitySupplier() {
		return new LinkQualitySupplier();
	}

	public FlowSupplier getFlowSupplier() {
		return new FlowSupplier();
	}

	private class NodeSupplier implements Supplier<Node> {
		@Override
		public Node get() {
			return new Node();
		}
	}

	private class LinkSupplier implements Supplier<Link<LinkQuality>> {
		@Override
		public Link<LinkQuality> get() {
			return new Link<LinkQuality>();
		}
	}

	private class LinkQualitySupplier extends EdgeWeightSupplier<LinkQuality> {
		@Override
		public LinkQuality get() {
			return new LinkQuality();
		}
	}

	public class FlowSupplier implements Supplier<Flow<Node, Link<LinkQuality>, LinkQuality>> {
		@Override
		public Flow<Node, Link<LinkQuality>, LinkQuality> get() {
			// TODO Auto-generated method stub
			return new Flow<Node, Link<LinkQuality>, LinkQuality>();
		}
	}
}