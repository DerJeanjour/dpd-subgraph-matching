package de.haw.testcase;

import de.haw.processing.GraphService;
import de.haw.repository.model.CpgEdgeType;
import de.haw.translation.CpgConst;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class GraphTestGenerator {

    public static Graph getEmpty() {
        return GraphService.instance().getEmptyGraph( id() );
    }

    public static Graph getSimpleGraph() {
        final GraphService GS = GraphService.instance();
        final Graph graph = getEmpty();

        final Node a = GS.addNode( graph, "1" );
        GS.addLabel( a, CpgConst.NODE_LABEL_DECLARATION_RECORD );
        final Node b = GS.addNode( graph, "2" );
        GS.addLabel( b, CpgConst.NODE_LABEL_DECLARATION_RECORD );
        final Node c = GS.addNode( graph, "3" );
        GS.addLabel( c, CpgConst.NODE_LABEL_DECLARATION_RECORD );
        final Node d = GS.addNode( graph, "4" );
        GS.addLabel( d, CpgConst.NODE_LABEL_DECLARATION_RECORD );

        final Edge ab = GS.addEdge( graph, "12", a, b );
        GS.setType( ab, CpgEdgeType.INTERACTS );
        final Edge bc = GS.addEdge( graph, "23", b, c );
        GS.setType( bc, CpgEdgeType.INTERACTS );
        final Edge cd = GS.addEdge( graph, "34", c, d );
        GS.setType( cd, CpgEdgeType.INTERACTS );
        final Edge ad = GS.addEdge( graph, "14", a, d );
        GS.setType( ad, CpgEdgeType.INTERACTS );

        return graph;
    }

    private static String id() {
        return GraphService.instance().genId( "test" );
    }

}
