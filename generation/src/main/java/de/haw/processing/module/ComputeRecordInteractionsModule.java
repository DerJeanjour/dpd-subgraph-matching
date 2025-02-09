package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.misc.utils.PathUtils;
import de.haw.processing.GraphService;
import de.haw.processing.model.CpgNodePaths;
import de.haw.processing.model.CpgPath;
import de.haw.processing.model.RecordInteraction;
import de.haw.processing.model.RecordInteractionType;
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
import java.util.Optional;
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

            final RecordInteraction interaction = this.getPathInteraction( recordPath );

            // add interaction node and edges
            final Node interactionNode = this.getInteractionOrCreate( graph, interaction );
            this.addEdgeForInteraction( graph, interactionNode, interaction.getTarget(), interaction, true );

            // mark existing path edges with path flag
            recordPath.getPath()
                    .getEdgePath()
                    .forEach( pathEdge -> graph.getEdge( pathEdge.getId() )
                            .setAttribute( CpgConst.EDGE_ATTR_IS_PATH, true ) );
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

    public Node getInteractionOrCreate(
            final Graph graph, final RecordInteraction interaction ) {

        final Optional<Edge> interactionEdge = graph.getNode( interaction.getSource().getId() )
                .leavingEdges()
                .filter( edge -> CpgEdgeType.INTERACTS.equals( this.GS.getType( edge ) ) )
                .filter( edge -> interaction.getType()
                        .name()
                        .equals( this.GS.getAttr( edge, CpgConst.EDGE_ATTR_INTERACTION_TYPE ) ) )
                .findFirst();

        Node interactionNode;
        if ( interactionEdge.isPresent() ) {
            return interactionEdge.get().getTargetNode();
        }

        interactionNode = this.GS.addNode( graph, String.valueOf( this.GS.genId() ) );
        interactionNode.setAttribute(
                CpgConst.NODE_ATTR_DATASET, this.GS.getAttr( graph, CpgConst.GRAPH_ATTR_DATASET ) );
        this.GS.addLabel( interactionNode, interaction.getType().name() );
        this.addEdgeForInteraction( graph, interaction.getSource(), interactionNode, interaction, false );
        return interactionNode;
    }

    private void addEdgeForInteraction(
            final Graph graph, final Node source, final Node target, final RecordInteraction interaction,
            final boolean applyPathAttrs ) {

        if ( source.leavingEdges().anyMatch( edge -> edge.getTargetNode().getId().equals( target.getId() ) ) ) {
            return;
        }

        final long edgeId = this.GS.genId();
        final Edge edge = this.GS.addEdge( graph, String.valueOf( edgeId ), source, target );
        this.GS.setType( edge, CpgEdgeType.INTERACTS );
        edge.setAttribute( CpgConst.EDGE_ATTR_INTERACTION_TYPE, interaction.getType().name() );
        edge.setAttribute( CpgConst.EDGE_ATTR_DATASET, this.GS.getAttr( graph, CpgConst.GRAPH_ATTR_DATASET ) );

        if ( applyPathAttrs ) {
            final String pathStr = PathUtils.pathToString( interaction.getPath().getPath(), true );
            edge.setAttribute( CpgConst.EDGE_ATTR_PATH, pathStr );
            edge.setAttribute( CpgConst.EDGE_ATTR_DISTANCE, interaction.getPath().getDistance() );

            source.setAttribute( CpgConst.NODE_ATTR_INTERACTION_COUNT, source.getOutDegree() );
        }


    }

    private RecordInteraction getPathInteraction( final CpgPath recordPath ) {

        final List<CpgEdgeType> pathTypes = PathUtils.getTypes( recordPath.getPath() );
        final Node pathSource = PathUtils.getFirstNode( recordPath.getPath() );
        final Node pathTarget = PathUtils.getLastNode( recordPath.getPath() );

        if ( pathTypes.contains( CpgEdgeType.INSTANTIATES ) ) {
            return RecordInteraction.of( RecordInteractionType.CREATES_RECORD, pathSource, pathTarget, recordPath );
        }
        if ( pathTypes.contains( CpgEdgeType.SUPER_TYPE_DECLARATIONS ) ) {
            return RecordInteraction.of( RecordInteractionType.EXTENDED_BY_RECORD, pathTarget, pathSource, recordPath );
        }
        if ( pathTypes.contains( CpgEdgeType.RETURN_TYPES ) ) {
            return RecordInteraction.of( RecordInteractionType.RETURNS_RECORD, pathSource, pathTarget, recordPath );
        }
        return RecordInteraction.of( RecordInteractionType.KNOWS_RECORD, pathSource, pathTarget, recordPath );
    }

}
