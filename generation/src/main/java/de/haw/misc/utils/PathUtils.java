package de.haw.misc.utils;

import de.haw.processing.GraphService;
import de.haw.repository.model.CpgEdgeType;
import de.haw.translation.CpgConst;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import java.util.List;
import java.util.Objects;

public class PathUtils {

    private static final GraphService GS = GraphService.instance();

    public static String pathToString( final Path path ) {
        return pathToString( path, false );
    }

    public static String pathToString( final Path path, final boolean withRecordScope ) {

        final StringBuilder sb = new StringBuilder();

        int iter = 0;
        for ( final Edge edge : path.getEdgePath() ) {

            iter++;

            sb.append( nodeToString( edge.getSourceNode(), withRecordScope ) );
            sb.append( edgeToString( edge ) );
            if ( iter == path.getEdgePath().size() ) {
                sb.append( nodeToString( edge.getTargetNode(), withRecordScope ) );
            }
        }

        return sb.toString().trim();
    }

    private static String edgeToString( final Edge edge ) {
        final StringBuilder sb = new StringBuilder();
        sb.append( "-[" );
        sb.append( GS.getType( edge ).name() );
        sb.append( "]->" );
        return sb.toString();
    }

    private static String nodeToString( final Node node, final boolean withRecordScope ) {
        final StringBuilder sb = new StringBuilder();
        sb.append( "(" );
        if ( withRecordScope ) {
            sb.append( GS.getAttr( node, CpgConst.NODE_ATTR_NAME_SCOPED_RECORD ) );
            sb.append( "::" );
        }
        sb.append( GS.getAttr( node, CpgConst.NODE_ATTR_NAME_LOCAL ) );
        sb.append( ")" );
        return sb.toString();
    }

    public static List<CpgEdgeType> getTypes( final Path path ) {
        return path.getEdgePath().stream().map( GS::getType ).filter( Objects::nonNull ).toList();
    }

    public static Node getFirstNode( final Path path ) {
        return path.getNodePath().stream().findFirst().orElse( null );
    }

    public static Node getLastNode( final Path path ) {
        if ( path.empty() ) {
            return null;
        }
        return path.getNodePath().get( path.getNodePath().size() - 1 );
    }

}
