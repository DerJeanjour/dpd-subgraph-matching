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

    private final GraphService GS = GraphService.instance();

    private final static int DEFAULT_NODE_SIZE = 2;

    private String sizeAttr;

    private Double sizeScale;

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {
        this.sizeAttr = ctx.get( PipeContext.NODE_SIZE_ATTR, String.class ).orElse( null );
        this.sizeScale = ctx.get( PipeContext.NODE_SIZE_SCALE, Double.class ).orElse( 1d );
        graph.nodes().forEach( this::setNodeStyle );
        return graph;
    }

    private boolean hasSizeAttr() {
        return this.sizeAttr != null;
    }

    private void setNodeStyle( final Node node ) {
        if ( this.GS.hasLabel( node, CpgConst.NODE_LABEL_SCOPE ) ) {
            GraphUi.clearStyle( node );
            GraphUi.addStyleParam( node, GraphUi.blueFill() );
            if ( !this.hasSizeAttr() ) {
                GraphUi.addStyleParam( node, GraphUi.getSizeParam( DEFAULT_NODE_SIZE * 5 ) );
            }
        }
        if ( this.GS.hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ) {
            GraphUi.clearStyle( node );
            GraphUi.addStyleParam( node, GraphUi.redFill() );
            if ( !this.hasSizeAttr() ) {
                GraphUi.addStyleParam( node, GraphUi.getSizeParam( DEFAULT_NODE_SIZE * 5 ) );
            }
        }
        if ( this.GS.hasLabel( node, CpgConst.NODE_LABEL_TRANSLATION_UNIT ) ) {
            GraphUi.clearStyle( node );
            GraphUi.addStyleParam( node, GraphUi.getFillColorParam( GraphUi.buildColorValue( Color.YELLOW ) ) );
            if ( !this.hasSizeAttr() ) {
                GraphUi.addStyleParam( node, GraphUi.getSizeParam( DEFAULT_NODE_SIZE * 5 ) );
            }
        }
        if ( this.GS.hasAnyLabel( node, DesignPatternType.getLabels() ) ) {
            GraphUi.clearStyle( node );
            GraphUi.addStyleParam( node, GraphUi.greenFill() );
            if ( !this.hasSizeAttr() ) {
                GraphUi.addStyleParam( node, GraphUi.getSizeParam( DEFAULT_NODE_SIZE * 5 ) );
            }
        }

        if ( this.hasSizeAttr() ) {
            double sizeValue = this.GS.getAttr( node, this.sizeAttr, Double.class ).orElse( 0d ) * this.sizeScale;
            final int nodeSize = DEFAULT_NODE_SIZE + Math.min( 50, Math.max( 0, ( int ) sizeValue ) );
            GraphUi.addStyleParam( node, GraphUi.getSizeParam( nodeSize ) );
        }
    }

}
