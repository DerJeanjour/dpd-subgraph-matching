package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import de.haw.repository.model.CpgEdgeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class SimplifyCpgEdgesModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final GraphService GS = GraphService.instance();

    // @formatter:off
    private static final List<CpgEdgeType> RELEVANT_EDGES = Arrays.asList(

            // aggregations
            CpgEdgeType.CDG,

            // elements
            CpgEdgeType.INITIALIZER,
            CpgEdgeType.FIELDS,
            CpgEdgeType.CONSTRUCTORS,
            CpgEdgeType.METHODS,
            CpgEdgeType.ARGUMENTS,
            CpgEdgeType.RETURN_VALUES,
            CpgEdgeType.RETURN_TYPES,
            CpgEdgeType.TYPE,

            // connection
            CpgEdgeType.RECORD_DECLARATION,

            // interactions
            CpgEdgeType.INSTANTIATES,
            CpgEdgeType.INVOKES,
            CpgEdgeType.REFERS_TO,

            // hierarchy
            CpgEdgeType.SUPER_TYPE_DECLARATIONS,
            CpgEdgeType.OVERRIDES
            //CpgEdgeType.PARENT
    );
    // @formatter:on

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        final Graph simplifiedGraph = this.GS.getEmptyGraph();

        graph.edges().forEach( edge -> {
            if ( this.GS.isAnyType( edge, RELEVANT_EDGES ) ) {
                this.GS.copyEdgeToGraph( simplifiedGraph, edge );
            }
        } );

        return simplifiedGraph;
    }

}
