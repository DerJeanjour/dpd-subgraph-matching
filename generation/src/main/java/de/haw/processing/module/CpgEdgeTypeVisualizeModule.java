package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.visualize.GraphUi;
import de.haw.repository.model.CpgEdgeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;

import java.awt.*;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class CpgEdgeTypeVisualizeModule<Target> extends PipeModule<Graph, Graph, Target> {

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        log.info( "Marking cpg edges by type with color ..." );

        graph.edges().forEach( edge -> {

            final String type = edge.getAttribute( "type", String.class );
            for ( CpgEdgeType edgeType : CpgEdgeType.values() ) {

                if ( type.equals( edgeType.name() ) ) {
                    this.markEdgeByType( edge, edgeType );
                }

            }

        } );

        return graph;
    }

    protected void markEdgeByType( final Edge edge, final CpgEdgeType edgeType ) {
        switch ( edgeType ) {
            case AST -> edge.setAttribute( GraphUi.ATTR_STYLE, GraphUi.greenFill() );
            case DFG -> edge.setAttribute( GraphUi.ATTR_STYLE, GraphUi.blueFill() );
            case EOG -> edge.setAttribute( GraphUi.ATTR_STYLE, GraphUi.redFill() );
            case CDG -> edge.setAttribute( GraphUi.ATTR_STYLE,
                    GraphUi.getFillColorParam( GraphUi.buildColorValue( Color.MAGENTA ) ) );
            case PDG -> edge.setAttribute( GraphUi.ATTR_STYLE,
                    GraphUi.getFillColorParam( GraphUi.buildColorValue( Color.CYAN ) ) );
        }
    }

}
