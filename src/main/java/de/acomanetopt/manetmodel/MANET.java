package de.acomanetopt.manetmodel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import de.jgraphlib.graph.UndirectedWeighted2DGraph;
import de.jgraphlib.util.Tuple;

public class MANET<N extends Node, L extends Link<W>, W extends LinkQuality> extends UndirectedWeighted2DGraph<N, L, W> {

	protected List<Flow<N, L, W>> flows;
	protected Supplier<Flow<N, L, W>> flowSupplier;
	protected IRadioModel radioModel;
	protected DataRate capacity;
	protected DataRate utilization;

	public MANET(Supplier<N> vertexSupplier, Supplier<L> edgeSupplier, Supplier<Flow<N, L, W>> flowSupplier,
			IRadioModel radioModel) {
		super(vertexSupplier, edgeSupplier);
		this.flowSupplier = flowSupplier;
		this.radioModel = radioModel;
		this.flows = new ArrayList<Flow<N, L, W>>();
		this.capacity = new DataRate(0L);
		this.utilization = new DataRate(0L);
	}
	
	public MANET(MANET<N,L,W> manet) {
		super(manet);
		this.flowSupplier = manet.flowSupplier;
		this.radioModel = manet.radioModel;	
		this.flows = new ArrayList<Flow<N, L, W>>();
		this.flows.addAll(manet.getFlows());		
		this.capacity = new DataRate(manet.capacity.get());		
		this.utilization = new DataRate(manet.utilization.get());
	}
	
	public MANET<N, L, W> copy() {
		return new MANET<N,L,W>(this);	
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
	
	public boolean addFlow(N source, N target, DataRate r) {
		return flows.add(new Flow<N, L, W>(source,target, r));
	}
	
	public List<Flow<N,L,W>> getFlows(){
		return flows;
	}

	public DataRate getUtilization() {
		return this.utilization;
	}

	public DataRate getCapacity() {
		return this.capacity;
	}
}
