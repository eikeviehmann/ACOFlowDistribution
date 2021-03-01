package de.multiobjective;

import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.ConsoleHandler;

import de.aco.Ant;
import de.aco.algorithms.multiobjective.ACOMultiObjective;
import de.aco.algorithms.multiobjective.AntAction;
import de.aco.algorithms.multiobjective.AntRequirement;
import de.jgraphlib.graph.Path;
import de.jgraphlib.graph.Path2D;
import de.jgraphlib.graph.Position2D;
import de.jgraphlib.graph.UndirectedWeighted2DGraph;
import de.jgraphlib.graph.UndirectedWeightedGraph;
import de.jgraphlib.graph.Vertex;
import de.jgraphlib.graph.WeightedEdge;
import de.jgraphlib.util.Triple;
import de.jgraphlib.util.Tuple;
import de.manetmodel.network.Flow;
import de.manetmodel.network.Link;
import de.manetmodel.network.LinkQuality;
import de.manetmodel.network.MANET;
import de.manetmodel.network.Node;
import de.manetmodel.network.unit.DataRate;

public class MinimumUtilization {

	//@formatter:off
	
	private MANET<Node, Link<LinkQuality>, LinkQuality> manet;
	private List<Flow<Node, Link<LinkQuality>, LinkQuality>> flows;
		
	public MinimumUtilization(MANET<Node, Link<LinkQuality>, LinkQuality> manet, List<Flow<Node, Link<LinkQuality>, LinkQuality>> flows) {
		this.manet = manet;
		this.flows = flows;			
	}

	public void run() {
		
		ACOMultiObjective<Node, Position2D, Link<LinkQuality>, LinkQuality> acoMultiObjective = 
				new ACOMultiObjective<Node, Position2D, Link<LinkQuality>, LinkQuality>(
					manet,
					(LinkQuality w) -> { return (double) w.getInterference(); }, 
					null,
					1000, 
					10);	
		
		for(Flow<Node, Link<LinkQuality>, LinkQuality> flow : flows)
			acoMultiObjective.addObjective(flow);
			
		acoMultiObjective.addAntRequirement(
				new AntRequirement<Node, Position2D, Link<LinkQuality>, LinkQuality> (){
					@Override
					public boolean check(Ant<Node, Position2D, Link<LinkQuality>, LinkQuality> ant, LinkQuality w) {
						return w.getUtilization().get() + new DataRate(0).get() <= w.getTransmissionRate().get();
					}		
				});		
		
		acoMultiObjective.addAntAction(
				new AntAction<Node, Position2D, Link<LinkQuality>, LinkQuality> (){
					@Override
					public void consume(Ant<Node, Position2D, Link<LinkQuality>, LinkQuality> ant, UndirectedWeightedGraph<Node, Position2D, Link<LinkQuality>, LinkQuality> graph) {
						for(Link<LinkQuality> link : manet.getEdge(ant.getPath().getLastEdge().getID()).inReceptionRange()) 
							link.setUtilization(new DataRate(0));	 			
					}		
				});				
	}
	
}
