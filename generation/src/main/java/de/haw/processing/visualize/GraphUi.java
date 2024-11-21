package de.haw.processing.visualize;

import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;

import java.awt.*;

public class GraphUi {

    /* CLASSES */

    public static final String CLASS_GRAPH = "graph";

    public static final String CLASS_NODE = "node";

    public static final String CLASS_EDGE = "edge";


    /* ATTRIBUTES */

    public static final String ATTR_STYLESHEET = "ui.stylesheet";

    public static final String ATTR_LABEL = "label";

    public static final String ATTR_STYLE = "ui.style";

    public static final String ATTR_QUALITY = "ui.quality";

    public static final String ATTR_AA = "ui.antialias";

    /* PARAMS */

    public static final String PARAM_FILL_COLOR = "fill-color";

    public static final String PARAM_PADDING = "padding";

    public static final String PARAM_SIZE = "size";

    public static final String PARAM_TEXT_MODE = "text-visibility-mode";

    public static final String PARAM_TEXT_SIZE = "text-size";

    public static final String PARAM_TEXT_STYLE = "text-style";

    public static final String PARAM_TEXT_COLOR = "text-color";

    public static final String PARAM_TEXT_ALIGNMENT = "text-alignment";

    public static final String PARAM_TEXT_PADDING = "text-padding";

    public static final String PARAM_TEXT_BACKGROUND_MODE = "text-background-mode";

    public static final String PARAM_TEXT_BACKGROUND_COLOR = "text-background-color";

    public static final String PARAM_SHAPE = "shape";

    public static final String PARAM_ARROW_SIZE = "arrow-size";


    /* VALUES */

    public static final String VALUE_COLOR_RED = "red";

    public static final String VALUE_COLOR_GREEN = "green";

    public static final String VALUE_COLOR_BLUE = "blue";

    public static final String VALUE_TEXT_STYLE_BOLD = "bold";

    public static final String VALUE_TEXT_ALIGNMENT_ABOVE = "above";

    public static final String VALUE_TEXT_ALIGNMENT_CENTER = "center";

    public static final String VALUE_TEXT_BACKGROUND_MODE_ROUNDED = "rounded-box";

    public static final String VALUE_TEXT_MODE_SHOW = "normal";

    public static final String VALUE_TEXT_MODE_HIDDEN = "hidden";

    public static final String VALUE_SHAPE_LINE = "line";

    public static String redFill() {
        return getFillColorParam( VALUE_COLOR_RED );
    }

    public static String greenFill() {
        return getFillColorParam( VALUE_COLOR_GREEN );
    }

    public static String blueFill() {
        return getFillColorParam( VALUE_COLOR_BLUE );
    }

    public static String grayFill() {
        return getFillColorParam( GraphUi.buildColorValue( Color.GRAY ) );
    }

    public static String getFillColorParam( final String color ) {
        return buildStyleProperty( PARAM_FILL_COLOR, color );
    }

    public static String buildStyleProperty( final String param, final String value ) {
        return param + ": " + value + ";";
    }

    public static String buildColorValue( final Color color ) {
        return "rgb(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")";
    }

    public static String getSizeParam( final int px ) {
        return buildStyleProperty( PARAM_SIZE, px + "px" );
    }

    public static void addStyleParam( final Element element, final String styleParam ) {
        if ( !element.hasAttribute( ATTR_STYLE, String.class ) ) {
            element.setAttribute( ATTR_STYLE, styleParam );
            return;
        }
        element.setAttribute( ATTR_STYLE, element.getAttribute( ATTR_STYLE, String.class ) + styleParam );
    }

    public static void clearStyle( final Element element ) {
        element.removeAttribute( ATTR_STYLE );
    }

    public static void display( final Graph graph, final boolean showLabels ) {

        if ( graph == null ) {
            return;
        }

        System.setProperty( "org.graphstream.ui", "swing" );

        graph.display();
        graph.setAttribute( GraphUi.ATTR_QUALITY );
        graph.setAttribute( GraphUi.ATTR_AA );

        GraphStyleBuilder builder = GraphStyleBuilder.builder();
        if ( !showLabels ) {
            builder = builder.nodeHideLabel();
        }
        builder = builder.graphPadding( 30 )
                .nodeSize( 5 )
                .nodeFillColor( Color.GRAY )
                .nodeLabelSize( 12 )
                .nodeBoldLabel()
                .nodeAlignLabelAbove()
                .nodeLabelPadding( 3 )
                .nodeLabelBackground( Color.CYAN )
                .nodeLabelTextColor( Color.DARK_GRAY )
                .edgeArrowSize( 5, 3 )
                .edgeLineShape();
        graph.setAttribute( GraphUi.ATTR_STYLESHEET, builder.build() );
    }

}
