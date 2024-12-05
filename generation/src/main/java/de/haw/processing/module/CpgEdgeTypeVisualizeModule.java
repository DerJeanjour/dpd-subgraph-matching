package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
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

    private final GraphService GS = GraphService.instance();


    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        log.info( "Marking cpg edges by type with color ..." );

        graph.edges().filter( edge -> this.GS.isAnyType( edge, CpgEdgeType.ALL ) ).forEach( this::markEdgeByType );

        return graph;
    }

    private void markEdgeByType( final Edge edge ) {

        final CpgEdgeType edgeType = this.GS.getType( edge );

        switch ( edgeType ) {
            case AST -> {
                GraphUi.clearStyle( edge );
                GraphUi.addStyleParam( edge, GraphUi.getSizeParam( 2 ) );
                GraphUi.addStyleParam( edge, GraphUi.greenFill() );
            }
            case DFG -> {
                GraphUi.clearStyle( edge );
                GraphUi.addStyleParam( edge, GraphUi.getSizeParam( 2 ) );
                GraphUi.addStyleParam( edge, GraphUi.blueFill() );
            }
            case EOG -> {
                GraphUi.clearStyle( edge );
                GraphUi.addStyleParam( edge, GraphUi.getSizeParam( 2 ) );
                GraphUi.addStyleParam( edge, GraphUi.redFill() );
            }
            case CDG -> {
                GraphUi.clearStyle( edge );
                GraphUi.addStyleParam( edge, GraphUi.getSizeParam( 2 ) );
                GraphUi.addStyleParam( edge, GraphUi.getFillColorParam( GraphUi.buildColorValue( Color.MAGENTA ) ) );
            }
            case PDG -> {
                GraphUi.clearStyle( edge );
                GraphUi.addStyleParam( edge, GraphUi.getSizeParam( 2 ) );
                GraphUi.addStyleParam( edge, GraphUi.getFillColorParam( GraphUi.buildColorValue( Color.CYAN ) ) );
            }
            default -> {
                GraphUi.clearStyle( edge );
                GraphUi.addStyleParam( edge, GraphUi.getSizeParam( 1 ) );
                GraphUi.addStyleParam( edge, GraphUi.grayFill() );
            }
        }
    }

}
