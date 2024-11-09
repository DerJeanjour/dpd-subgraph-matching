package de.haw.processing.graph;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * <a href="https://graphstream-project.org/doc/Advanced-Concepts/GraphStream-CSS-Reference/">GraphStream CSS</a>
 */
public class GraphStyleBuilder {

    private final Map<String, String> graphStyle = new HashMap<>();

    private final Map<String, String> nodeStyle = new HashMap<>();

    private final Map<String, String> edgeStyle = new HashMap<>();

    public static GraphStyleBuilder builder() {
        return new GraphStyleBuilder();
    }

    public GraphStyleBuilder graphPadding( final int pixel ) {
        this.graphStyle.put( GraphUi.PARAM_PADDING, pixel + "px" );
        return this;
    }

    public GraphStyleBuilder nodeSize( final int pixel ) {
        this.nodeStyle.put( GraphUi.PARAM_SIZE, pixel + "px" );
        return this;
    }

    public GraphStyleBuilder nodeFillColor( final Color color ) {
        this.nodeStyle.put( GraphUi.PARAM_FILL_COLOR, GraphUi.buildColorValue( color ) );
        return this;
    }

    public GraphStyleBuilder nodeHideLabel() {
        this.nodeStyle.put( GraphUi.PARAM_TEXT_MODE, GraphUi.VALUE_TEXT_MODE_HIDDEN );
        return this;
    }

    public GraphStyleBuilder nodeLabelSize( final int pixel ) {
        this.nodeStyle.put( GraphUi.PARAM_TEXT_SIZE, pixel + "px" );
        return this;
    }

    public GraphStyleBuilder nodeLabelPadding( final int pixel ) {
        this.nodeStyle.put( GraphUi.PARAM_TEXT_PADDING, pixel + "px" );
        return this;
    }

    public GraphStyleBuilder nodeLabelBackground( final Color color ) {
        this.nodeStyle.put(
                GraphUi.PARAM_TEXT_BACKGROUND_MODE, GraphUi.VALUE_TEXT_BACKGROUND_MODE_ROUNDED );
        this.nodeStyle.put( GraphUi.PARAM_TEXT_BACKGROUND_COLOR, GraphUi.buildColorValue( color ) );
        return this;
    }

    public GraphStyleBuilder nodeBoldLabel() {
        this.nodeStyle.put( GraphUi.PARAM_TEXT_STYLE, GraphUi.VALUE_TEXT_STYLE_BOLD );
        return this;
    }

    public GraphStyleBuilder nodeAlignLabelAbove() {
        this.nodeStyle.put( GraphUi.PARAM_TEXT_ALIGNMENT, GraphUi.VALUE_TEXT_ALIGNMENT_ABOVE );
        return this;
    }

    public GraphStyleBuilder nodeLabelTextColor( final Color color ) {
        this.nodeStyle.put( GraphUi.PARAM_TEXT_COLOR, GraphUi.buildColorValue( color ) );
        return this;
    }

    public GraphStyleBuilder edgeLineShape() {
        this.edgeStyle.put( GraphUi.PARAM_SHAPE, GraphUi.VALUE_SHAPE_LINE );
        return this;
    }

    public GraphStyleBuilder edgeArrowSize( final int pixelX, final int pixelY ) {
        this.edgeStyle.put( GraphUi.PARAM_ARROW_SIZE, pixelX + "px, " + pixelY + "px" );
        return this;
    }

    public String build() {

        final StringBuilder builder = new StringBuilder();

        builder.append( GraphUi.CLASS_GRAPH ).append( " { " );
        graphStyle.forEach(
                ( key, value ) -> builder.append( GraphUi.buildStyleProperty( key, value ) ).append( " " ) );
        builder.append( "} " );

        builder.append( GraphUi.CLASS_NODE ).append( " { " );
        nodeStyle.forEach(
                ( key, value ) -> builder.append( GraphUi.buildStyleProperty( key, value ) ).append( " " ) );
        builder.append( "} " );

        builder.append( GraphUi.CLASS_EDGE ).append( " { " );
        edgeStyle.forEach(
                ( key, value ) -> builder.append( GraphUi.buildStyleProperty( key, value ) ).append( " " ) );
        builder.append( "} " );
        return builder.toString().trim();
    }

}
