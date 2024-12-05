package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.misc.utils.PathUtils;
import de.haw.processing.GraphService;
import de.haw.repository.model.CpgEdgeType;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.*;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class ComputeRecordDependenciesModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final GraphService GS = GraphService.instance();

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        if ( ctx.get( PipeContext.RECORD_PATHS, HashMap.class ).isEmpty() ) {
            log.info( "Record paths are not present ..." );
            return graph;
        }

        final Map<String, List<ComputeSSSPsModule.Data>> recordPaths = getPaths( ctx );

        recordPaths.values().forEach( paths -> paths.forEach( recordPath -> {

            if ( !this.isValidPath( recordPath ) ) {
                return;
            }

            final String edgeId = this.GS.genId( "edge" );
            final Edge edge = this.GS.addEdge( graph, edgeId, recordPath.getSource(), recordPath.getTarget() );
            this.GS.setType( edge, CpgEdgeType.RECORD_KNOWS );
            edge.setAttribute( CpgConst.EDGE_ATTR_DISTANCE, recordPath.getDistance() );
            recordPath.getPath().getEdgePath().forEach( pathEdge -> pathEdge.setAttribute( "isPath", true ) );

            final String pathStr = PathUtils.pathToString( recordPath.getPath(), true );
            edge.setAttribute( "path", pathStr );
            //log.info( "Got path: {}", pathStr );

        } ) );

        return graph;
    }

    @SuppressWarnings( "unchecked" )
    private Map<String, List<ComputeSSSPsModule.Data>> getPaths( final PipeContext ctx ) {
        return ctx.get( PipeContext.RECORD_PATHS, HashMap.class ).orElse( new HashMap<>() );
    }

    private boolean isValidPath( final ComputeSSSPsModule.Data recordPath ) {

        // validate path distance
        if ( recordPath.getPath().empty() ) {
            return false;
        }

        int iter = 0;
        final Set<String> traversedRecordScopes = new HashSet<>();
        final List<Node> nodePath = recordPath.getPath().getNodePath();
        for ( final Node node : nodePath ) {
            iter++;

            final String recordScope = this.GS.getAttr( node, CpgConst.NODE_ATTR_NAME_SCOPED_RECORD );
            if ( StringUtils.isNotBlank( recordScope ) ) {
                traversedRecordScopes.add( recordScope );
            }

            // check if scope (package + classname) changes more than 2 times
            if ( traversedRecordScopes.size() > 2 ) {
                return false;
            }

            if ( iter == 1 || iter == nodePath.size() ) {
                // ignore path source and target nodes
                continue;
            }

            // check if path contains any record nodes
            if ( this.GS.hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ) {
                return false;
            }
        }

        return true;
    }

}
