package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.misc.utils.CollectionUtils;
import de.haw.misc.utils.PathUtils;
import de.haw.processing.GraphService;
import de.haw.processing.model.*;
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
public class ComputeRecordInteractionsModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final GraphService GS = GraphService.instance();


    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        if ( ctx.get( PipeContext.RECORD_PATHS, CpgNodePaths.class ).isEmpty() ) {
            log.info( "Record paths are not present ..." );
            return graph;
        }

        final CpgNodePaths recordPaths = getPaths( ctx );
        final RecordInteractionPathCounts pathCounts = RecordInteractionPathCounts.instance();
        final Map<String, List<RecordInteraction>> recordInteractionsBySource = new HashMap<>();
        recordPaths.getAll().forEach( path -> {
            if ( !this.isValidPath( path ) ) {
                return;
            }
            final RecordInteraction interaction = this.getPathInteraction( path );
            final String sourceId = path.getSource().getId();
            if ( !recordInteractionsBySource.containsKey( sourceId ) ) {
                recordInteractionsBySource.put( sourceId, new ArrayList<>() );
            }
            recordInteractionsBySource.get( sourceId ).add( interaction );
        } );

        recordInteractionsBySource.keySet().forEach( sourceId -> {
            final List<RecordInteraction> interactions = recordInteractionsBySource.get( sourceId );
            final List<RecordInteraction> recordInteractions = this.reduceSourceInteractions( interactions );
            recordInteractions.forEach( interaction -> {
                pathCounts.add( interaction );
                // add interaction node and edges
                final Node interactionNode = this.getInteractionOrCreate( graph, interaction );
                this.addEdgeForInteraction( graph, interactionNode, interaction.getTarget(), interaction, true );
                // mark existing path edges with path flag
                interaction.getPath()
                        .getPath()
                        .getEdgePath()
                        .forEach( pathEdge -> graph.getEdge( pathEdge.getId() )
                                .setAttribute( CpgConst.EDGE_ATTR_IS_PATH, true ) );

            } );

        } );

        //GraphUi.display( pathCounts.toGraph(), false );
        return graph;
    }

    public CpgNodePaths getPaths( final PipeContext ctx ) {
        return ctx.get( PipeContext.RECORD_PATHS, CpgNodePaths.class ).orElseThrow( IllegalStateException::new );
    }

    public boolean isValidPath( final CpgPath recordPath ) {

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

    public void addEdgeForInteraction(
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

    public RecordInteraction getPathInteraction( final CpgPath recordPath ) {

        final List<CpgEdgeType> pathTypes = PathUtils.getTypes( recordPath.getPath() );
        final Node pathSource = PathUtils.getFirstNode( recordPath.getPath() );
        final Node pathTarget = PathUtils.getLastNode( recordPath.getPath() );

        RecordInteractionDescriptor matchedDescriptor = RecordInteractionDescriptor.KNOWS_INTERACTION;
        for ( final RecordInteractionDescriptor descriptor : RecordInteractionDescriptor.ALL ) {
            if ( descriptor.hasEdgePivot( pathTypes ) && descriptor.getOrder() < matchedDescriptor.getOrder() ) {
                matchedDescriptor = descriptor;
            }
        }
        final Node source = matchedDescriptor.isReversedInteraction() ? pathTarget : pathSource;
        final Node target = matchedDescriptor.isReversedInteraction() ? pathSource : pathTarget;
        return RecordInteraction.of( matchedDescriptor.getType(), source, target, recordPath );
    }

    public List<RecordInteraction> reduceSourceInteractions( final List<RecordInteraction> interactions ) {
        final Map<String, List<RecordInteraction>> interactionsByTarget = new HashMap<>();
        final Map<String, List<RecordInteractionType>> allowedTypesByTarget = new HashMap<>();
        for ( final RecordInteraction interaction : interactions ) {

            final RecordInteractionDescriptor descriptor = RecordInteractionDescriptor.get( interaction.getType() );

            final String targetId = descriptor.isReversedInteraction() ? interaction.getSource()
                    .getId() : interaction.getTarget().getId();
            if ( !interactionsByTarget.containsKey( targetId ) ) {
                interactionsByTarget.put( targetId, new ArrayList<>() );
                allowedTypesByTarget.put( targetId, RecordInteractionType.all() );
            }
            interactionsByTarget.get( targetId ).add( interaction );


            final List<RecordInteractionType> typeIntersection = CollectionUtils.intersection(
                    allowedTypesByTarget.get( targetId ), descriptor.getAllowedSiblings() );
            allowedTypesByTarget.put( targetId, typeIntersection );
        }

        final List<RecordInteraction> reduced = new ArrayList<>();
        interactionsByTarget.keySet().forEach( targetId -> {
            for ( final RecordInteraction targetInteraction : interactionsByTarget.get( targetId ) ) {
                final List<RecordInteractionType> allowedTypes = allowedTypesByTarget.get( targetId );
                if ( allowedTypes.contains( targetInteraction.getType() ) ) {
                    reduced.add( targetInteraction );
                }
            }
        } );
        return reduced;
    }

}
