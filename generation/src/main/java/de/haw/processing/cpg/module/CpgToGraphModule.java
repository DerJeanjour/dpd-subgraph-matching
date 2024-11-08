package de.haw.processing.cpg.module;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg_vis_neo4j.Application;
import de.fraunhofer.aisec.cpg_vis_neo4j.JsonEdge;
import de.fraunhofer.aisec.cpg_vis_neo4j.JsonGraph;
import de.fraunhofer.aisec.cpg_vis_neo4j.JsonNode;
import de.haw.processing.pipe.PipeModule;
import de.haw.utils.ReflectionUtils;
import kotlin.Pair;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.neo4j.ogm.cypher.compiler.builders.node.DefaultNodeBuilder;
import org.neo4j.ogm.cypher.compiler.builders.node.DefaultRelationshipBuilder;

import java.util.List;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class CpgToGraphModule<Target> extends PipeModule<TranslationResult, Graph, Target> {

    @Override
    protected Graph processImpl( final TranslationResult result ) {

        log.info( "Persisting parsed results ..." );
        if ( result == null ) {
            throw new IllegalArgumentException( "Can't persist null result!" );
        }

        final Application neo4j = new Application();
        ReflectionUtils.setInt( neo4j, "depth", 10 );

        final Pair<List<DefaultNodeBuilder>, List<DefaultRelationshipBuilder>> jsonData = neo4j.translateCPGToOGMBuilders(
                result );
        return this.toGraph( neo4j.buildJsonGraph( jsonData.getFirst(), jsonData.getSecond() ) );
    }

    private Graph toGraph( final JsonGraph json ) {
        final MultiGraph graph = new MultiGraph( "graph", false, false );
        for ( JsonNode jsonNode : json.getNodes() ) {
            Node node = graph.addNode( String.valueOf( jsonNode.getId() ) );
            node.setAttributes( jsonNode.getProperties() );
            node.setAttribute( "labels", jsonNode.getLabels() );
        }
        for ( JsonEdge jsonEdge : json.getEdges() ) {
            Edge edge = graph.addEdge( String.valueOf( jsonEdge.getId() ), String.valueOf( jsonEdge.getStartNode() ),
                    String.valueOf( jsonEdge.getEndNode() ) );
            edge.setAttributes( jsonEdge.getProperties() );
            edge.setAttribute( "type", jsonEdge.getType() );
        }
        return graph;
    }

}
