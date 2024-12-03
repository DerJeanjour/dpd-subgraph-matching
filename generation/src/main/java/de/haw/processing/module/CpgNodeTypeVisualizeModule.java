package de.haw.processing.module;

import de.haw.dataset.model.DesignPatternType;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import de.haw.processing.visualize.GraphUi;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.awt.*;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class CpgNodeTypeVisualizeModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final GraphService graphService = GraphService.instance();

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {
        graph.nodes().forEach( this::setNodeStyle );
        return graph;
    }

    private void setNodeStyle( final Node node ) {
        if ( this.graphService.hasLabel( node, CpgConst.NODE_LABEL_SCOPE ) ) {
            GraphUi.clearStyle( node );
            GraphUi.addStyleParam( node, GraphUi.blueFill() );
            GraphUi.addStyleParam( node, GraphUi.getSizeParam( 10 ) );
        }
        if ( this.graphService.hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ) {
            GraphUi.clearStyle( node );
            GraphUi.addStyleParam( node, GraphUi.redFill() );
            GraphUi.addStyleParam( node, GraphUi.getSizeParam( 10 ) );
        }
        if ( this.graphService.hasLabel( node, CpgConst.NODE_LABEL_TRANSLATION_UNIT ) ) {
            GraphUi.clearStyle( node );
            GraphUi.addStyleParam( node, GraphUi.getFillColorParam( GraphUi.buildColorValue( Color.YELLOW ) ) );
            GraphUi.addStyleParam( node, GraphUi.getSizeParam( 10 ) );
        }
        if ( this.graphService.hasAnyLabel( node, DesignPatternType.getLabels() ) ) {
            GraphUi.clearStyle( node );
            GraphUi.addStyleParam( node, GraphUi.greenFill() );
            GraphUi.addStyleParam( node, GraphUi.getSizeParam( 10 ) );
        }
    }

}
