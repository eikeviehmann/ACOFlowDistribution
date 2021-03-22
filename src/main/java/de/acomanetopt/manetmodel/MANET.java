package de.acomanetopt.manetmodel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import de.jgraphlib.graph.UndirectedWeighted2DGraph;
import de.jgraphlib.util.Tuple;

public class MANET<N extends Node, L extends Link<W>, W extends LinkQuality>
		extends UndirectedWeighted2DGraph<N, L, W> {

	protected List<Flow<N, L, W>> flows;
	protected IRadioModel radioModel;
	protected DataRate capacity;
	protected DataRate utilization;

	public MANET(Supplier<N> vertexSupplier, Supplier<L> edgeSupplier, Supplier<W> edgeWeightSupplier,
			IRadioModel radioModel) {
		super(vertexSupplier, edgeSupplier);
		setEdgeWeightSupplier(edgeWeightSupplier);

		this.radioModel = radioModel;
		this.flows = new ArrayList<Flow<N, L, W>>();
		this.capacity = new DataRate(0L);
		this.utilization = new DataRate(0L);
	}

	public MANET(MANET<N, L, W> manet) {
		super(manet.vertexSupplier, manet.edgeSupplier);

		// Shallow copy (are not alternated within the process)
		this.edgeWeightSupplier = manet.edgeWeightSupplier;
		this.vertices = manet.vertices;
		this.vertexAdjacencies = manet.vertexAdjacencies;
		this.edgeAdjacencies = manet.edgeAdjacencies;
		this.radioModel = manet.radioModel;
	
		// Deep copy edges
		for (L l : manet.getEdges()) {
			L lCopy = edgeSupplier.get();
			lCopy.setID(l.getID());
			lCopy.setUtilizedLinks(l.getUtilizedLinkIds());
			lCopy.setIsActive(l.getIsActive());
			W wCopy = edgeWeightSupplier.get();
			wCopy.setDistance(l.getWeight().getDistance());
			wCopy.setReceptionPower(l.getWeight().getReceptionPower());
			wCopy.setTransmissionRate(new DataRate(l.getWeight().getTransmissionRate().get()));
			wCopy.setUtilization(new DataRate(l.getWeight().getUtilization().get()));
			wCopy.setUtilizedLinks(l.getUtilizedLinkIds().size());
			lCopy.setWeight(wCopy);
			edges.add(lCopy);
		}
		
		// Deep copy flows
		//this.flows = manet.flows;
		this.flows = new ArrayList<Flow<N,L,W>>();
		for(Flow<N,L,W> f : manet.flows) {
			
			Flow<N,L,W> fCopy = new Flow<N,L,W>(getVertex(f.getSource().getID()), getVertex(f.getTarget().getID()), f.getDataRate());
			
			for(Tuple<L,N> tuple : f)		
				if(tuple.getFirst() != null)
					fCopy.add(new Tuple<L,N>(getEdge(tuple.getFirst().getID()), getVertex(tuple.getSecond().getID())));		
				else
					fCopy.add(new Tuple<L,N>(null, getVertex(tuple.getSecond().getID())));	
			
			this.flows.add(fCopy);
		}
				
		// Deep copy capacity
		this.capacity = new DataRate(manet.capacity.get());
		
		// Deep copy utilization
		this.utilization = new DataRate(manet.utilization.get());
	}

	public MANET<N, L, W> copy() {
		/* This is a deep copy */
		return new MANET<N, L, W>(this);
	}

	@Override
	public L addEdge(N source, N target, W weight) {
		L link = super.addEdge(source, target, weight);

		for (L l : this.getEdges()) {
			Tuple<N, N> lt = this.getVerticesOf(l);
			N s1 = lt.getFirst();
			N s2 = lt.getSecond();

			double distance = this.getDistance(s1.getPosition(), s2.getPosition());

			if (l == link)
				capacity.set(capacity.get() + radioModel.transmissionBitrate(distance).get());
			l.getWeight().setTransmissionRate(radioModel.transmissionBitrate(distance));
			l.getWeight().setReceptionPower(radioModel.receptionPower(distance));

			List<N> lListOfs1 = this.getNextHopsOf(s1);
			List<N> lListOfs2 = this.getNextHopsOf(s2);

			for (N n : lListOfs1)
				l.setUtilizedLinks(new HashSet<Integer>(this.getEdgeIdsOf(n.getID())));

			for (N n : lListOfs2)
				l.setUtilizedLinks(new HashSet<Integer>(this.getEdgeIdsOf(n.getID())));

			l.getWeight().setUtilizedLinks(l.getUtilizedLinkIds().size());
		}
		return link;
	}

	public void increaseUtilizationBy(L l, DataRate r) {
		Set<Integer> interferedLinks = new HashSet<Integer>(l.getUtilizedLinkIds());
		Iterator<Integer> lIdIterator = interferedLinks.iterator();
		while (lIdIterator.hasNext()) {
			this.utilization.set(this.utilization.get() + r.get());
			L interferedLink = this.getEdge(lIdIterator.next());
			DataRate linkUtilization = interferedLink.getWeight().getUtilization();
			interferedLink.getWeight().setUtilization(new DataRate(linkUtilization.get() + r.get()));
		}
	}

	public void deployFlow(Flow<N, L, W> flow) {
		
		this.flows.add(flow);
		
		Iterator<Tuple<L, N>> flowIterator = flow.listIterator(1);

		while (flowIterator.hasNext()) {
			Tuple<L, N> linkAndNode = flowIterator.next();
			L l = linkAndNode.getFirst();
			l.setIsActive(true);
			increaseUtilizationBy(l, flow.getDataRate());
		}
	}
	
	public void addFlow(Flow<N, L, W> flow) {
		this.flows.add(flow);
	}

	public List<Flow<N, L, W>> getFlows() {
		return flows;
	}

	public DataRate getUtilization() {
		return this.utilization;
	}

	public DataRate getCapacity() {
		return this.capacity;
	}
}
