package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.misc.utils.PathUtils;
import de.haw.processing.GraphService;
import de.haw.processing.model.CpgNodePaths;
import de.haw.processing.model.CpgPath;
import de.haw.repository.model.CpgEdgeType;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class ComputeRecordInteractionsModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final GraphService GS = GraphService.instance();

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        if ( ctx.get( PipeContext.RECORD_PATHS, CpgNodePaths.class ).isEmpty() ) {
            log.info( "Record paths are not present ..." );
            return graph;
        }

        final CpgNodePaths recordPaths = getPaths( ctx );

        recordPaths.getAll().forEach( recordPath -> {

            if ( !this.isValidPath( recordPath ) ) {
                return;
            }

            final String edgeId = this.GS.genId( "edge" );
            final Edge edge = this.GS.addEdge( graph, edgeId, recordPath.getSource(), recordPath.getTarget() );
            this.GS.setType( edge, this.getTypeOfPath( recordPath ) );
            edge.setAttribute( CpgConst.EDGE_ATTR_DISTANCE, recordPath.getDistance() );
            recordPath.getPath()
                    .getEdgePath()
                    .forEach( pathEdge -> pathEdge.setAttribute( CpgConst.EDGE_ATTR_IS_PATH, true ) );

            final String pathStr = PathUtils.pathToString( recordPath.getPath(), true );
            edge.setAttribute( CpgConst.EDGE_ATTR_PATH, pathStr );
            //log.info( "Got path: {}", pathStr );

        } );

        return graph;
    }

    private CpgNodePaths getPaths( final PipeContext ctx ) {
        return ctx.get( PipeContext.RECORD_PATHS, CpgNodePaths.class ).orElseThrow( IllegalStateException::new );
    }

    private boolean isValidPath( final CpgPath recordPath ) {

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

    private CpgEdgeType getTypeOfPath( final CpgPath recordPath ) {
        final List<CpgEdgeType> pathTypes = PathUtils.getTypes( recordPath.getPath() );
        if ( pathTypes.contains( CpgEdgeType.INSTANTIATES ) ) {
            return CpgEdgeType.RECORD_CREATES;
        }
        if ( pathTypes.contains( CpgEdgeType.SUPER_TYPE_DECLARATIONS ) ) {
            return CpgEdgeType.RECORD_EXTENDS;
        }
        if ( pathTypes.contains( CpgEdgeType.RETURN_TYPES ) ) {
            return CpgEdgeType.RECORD_RETURNS;
        }
        return CpgEdgeType.RECORD_KNOWS;
    }

}
