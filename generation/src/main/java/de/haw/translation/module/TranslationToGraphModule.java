package de.haw.translation.module;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg_vis_neo4j.Application;
import de.fraunhofer.aisec.cpg_vis_neo4j.JsonEdge;
import de.fraunhofer.aisec.cpg_vis_neo4j.JsonGraph;
import de.fraunhofer.aisec.cpg_vis_neo4j.JsonNode;
import de.haw.dataset.model.Dataset;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.misc.utils.ReflectionUtils;
import de.haw.processing.GraphService;
import de.haw.translation.CpgConst;
import kotlin.Pair;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.neo4j.ogm.cypher.compiler.builders.node.DefaultNodeBuilder;
import org.neo4j.ogm.cypher.compiler.builders.node.DefaultRelationshipBuilder;

import java.util.List;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class TranslationToGraphModule<Target> extends PipeModule<TranslationResult, Graph, Target> {

    private static final boolean PREVENT_SELF_LOOPS = true;

    private final GraphService GS = GraphService.instance();

    @Override
    protected Graph processImpl( final TranslationResult result, final PipeContext ctx ) {

        if ( result == null ) {
            throw new IllegalArgumentException( "Can't convert null result!" );
        }

        final int minDepth = ctx.get( PipeContext.CPG_MIN_DEPTH_KEY, 8, Integer.class );
        int depth = ctx.get( PipeContext.CPG_DEPTH_KEY, 10, Integer.class );

        while ( depth >= minDepth ) {
            try {
                return this.translateGraph( result, ctx, depth );
            } catch ( Exception e ) {
                log.error( "Failed to parse CPG in depth {}: {}", depth, e.getMessage() );
                depth--;
            } catch ( OutOfMemoryError e ) {
                // TODO this is very unsafe and will not guarantee correct process
                log.error( "Failed to parse CPG in depth {}: {}", depth, e.getMessage() );
                System.gc();
                depth--;
            }
        }

        throw new IllegalStateException( "Couldn't parse CPG results with min depth of " + minDepth );
    }

    public Graph translateGraph( final TranslationResult result, final PipeContext ctx, final int depth ) {
        log.info( "Converting CPG result to graph with depth {} ...", depth );
        final Application neo4j = new Application();
        ReflectionUtils.setInt( neo4j, "depth", depth );
        final Pair<List<DefaultNodeBuilder>, List<DefaultRelationshipBuilder>> jsonData = neo4j.translateCPGToOGMBuilders(
                result );

        final Graph graph = this.toGraph( neo4j.buildJsonGraph( jsonData.getFirst(), jsonData.getSecond() ), ctx );
        if ( this.graphIsEmpty( graph ) ) {
            throw new IllegalStateException( "Translated graph is empty." );
        }
        return graph;
    }

    public Graph toGraph( final JsonGraph json, final PipeContext ctx ) {

        final Dataset dataset = ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class )
                .orElseThrow( IllegalStateException::new );
        final Graph graph = this.GS.getEmptyGraph( dataset.getName() );
        graph.setAttribute( CpgConst.GRAPH_ATTR_DATASET, dataset.getName() );

        for ( JsonNode jsonNode : json.getNodes() ) {
            Node node = graph.addNode( String.valueOf( jsonNode.getId() ) );
            if ( node == null ) {
                node = graph.getNode( String.valueOf( jsonNode.getId() ) );
            }
            if ( node != null ) {
                node.setAttributes( jsonNode.getProperties() );
                node.setAttribute( CpgConst.NODE_ATTR_LABELS, jsonNode.getLabels() );
                node.setAttribute( CpgConst.NODE_ATTR_DATASET, dataset.getName() );
            }
        }
        for ( JsonEdge jsonEdge : json.getEdges() ) {

            final String edgeId = String.valueOf( jsonEdge.getId() );
            final String sourceNode = String.valueOf( jsonEdge.getStartNode() );

            final String targetNode = String.valueOf( jsonEdge.getEndNode() );
            if ( PREVENT_SELF_LOOPS && sourceNode.equals( targetNode ) ) {
                continue;
            }

            Edge edge = graph.addEdge( edgeId, sourceNode, targetNode, true );
            if ( edge == null ) {
                edge = graph.getEdge( edgeId );
            }
            if ( edge != null ) {
                edge.setAttributes( jsonEdge.getProperties() );
                edge.setAttribute( CpgConst.EDGE_ATTR_TYPE, jsonEdge.getType() );
                if ( StringUtils.isNotBlank( jsonEdge.getType() ) ) {
                    edge.setAttribute( CpgConst.EDGE_ATTR_LABEL, jsonEdge.getType() );
                }
                edge.setAttribute( CpgConst.EDGE_ATTR_DATASET, dataset.getName() );
            }
        }

        return graph;
    }

    public boolean graphIsEmpty( final Graph graph ) {
        return graph == null || graph.getNodeCount() == 0;
    }

}
