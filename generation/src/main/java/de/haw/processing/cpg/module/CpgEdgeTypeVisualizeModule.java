package de.haw.processing.cpg.module;

import de.haw.processing.cpg.model.CpgEdgeType;
import de.haw.processing.graph.GraphUi;
import de.haw.processing.pipe.PipeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class CpgEdgeTypeVisualizeModule<Target> extends PipeModule<Graph, Graph, Target> {

    @Override
    protected Graph processImpl( final Graph graph ) {

        log.info( "Marking cpg edges by type with color ..." );

        graph.edges().forEach( edge -> {

            final String type = edge.getAttribute( "type", String.class );
            for ( CpgEdgeType edgeType : CpgEdgeType.values() ) {

                if ( type.startsWith( edgeType.getValue() ) ) {
                    this.markEdgeByType( edge, edgeType );
                }

            }

        } );

        return graph;
    }

    protected void markEdgeByType( final Edge edge, final CpgEdgeType edgeType ) {
        switch ( edgeType ) {
            case ABSTRACT_SYNTAX_TREE -> edge.setAttribute( GraphUi.ATTR_STYLE, GraphUi.greenFill() );
            case DATA_FLOW_GRAPH -> edge.setAttribute( GraphUi.ATTR_STYLE, GraphUi.blueFill() );
            case EVALUATION_ORDER_GRAPH -> edge.setAttribute( GraphUi.ATTR_STYLE, GraphUi.redFill() );
        }
    }

}
