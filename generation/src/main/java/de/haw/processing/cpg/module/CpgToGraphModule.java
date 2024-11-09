package de.haw.processing.cpg.module;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg_vis_neo4j.Application;
import de.fraunhofer.aisec.cpg_vis_neo4j.JsonEdge;
import de.fraunhofer.aisec.cpg_vis_neo4j.JsonGraph;
import de.fraunhofer.aisec.cpg_vis_neo4j.JsonNode;
import de.haw.processing.graph.GraphService;
import de.haw.processing.pipe.PipeModule;
import de.haw.utils.ReflectionUtils;
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
public class CpgToGraphModule<Target> extends PipeModule<TranslationResult, Graph, Target> {

    private final boolean PREVENT_SELF_LOOPS = true;

    private final GraphService graphService = GraphService.instance();

    @Override
    protected Graph processImpl( final TranslationResult result ) {

        log.info( "Converting result to graph ..." );
        if ( result == null ) {
            throw new IllegalArgumentException( "Can't convert null result!" );
        }

        final Application neo4j = new Application();
        ReflectionUtils.setInt( neo4j, "depth", 10 );

        final Pair<List<DefaultNodeBuilder>, List<DefaultRelationshipBuilder>> jsonData = neo4j.translateCPGToOGMBuilders(
                result );
        return this.toGraph( neo4j.buildJsonGraph( jsonData.getFirst(), jsonData.getSecond() ) );
    }

    private Graph toGraph( final JsonGraph json ) {

        final Graph graph = this.graphService.getEmptyGraph();

        for ( JsonNode jsonNode : json.getNodes() ) {
            Node node = graph.addNode( String.valueOf( jsonNode.getId() ) );
            if ( node == null ) {
                node = graph.getNode( String.valueOf( jsonNode.getId() ) );
            }
            if ( node != null ) {
                node.setAttributes( jsonNode.getProperties() );
                node.setAttribute( "labels", jsonNode.getLabels() );
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
                edge.setAttribute( "type", jsonEdge.getType() );
                if ( StringUtils.isNotBlank( jsonEdge.getType() ) ) {
                    edge.setAttribute( "label", jsonEdge.getType() );
                }
            }
        }

        return graph;
    }

}
