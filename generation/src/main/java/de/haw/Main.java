package de.haw;

import de.haw.datasets.Dataset;
import de.haw.processing.cpg.CpgProcessor;
import de.haw.processing.cpg.module.CpgGenerateModule;
import de.haw.processing.cpg.module.CpgToGraphModule;
import de.haw.processing.graph.module.PersistToNeo4jModule;
import de.haw.processing.pipe.PipeBuilder;
import de.haw.processing.pipe.PipeModule;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;

@Slf4j
public class Main {

    public static void main( String[] args ) {

        /*
        final CpgProcessor processor = new CpgProcessor();
        final Graph cpg = processor.run( Dataset.ANIMAL );

         */

        /*
        final PipeModule<Dataset, ?, Graph> pipe = PipeBuilder.<Dataset, Graph>builder()
                .add( CpgGenerateModule.instance() )
                .add( CpgToGraphModule.instance() )
                //.add( CpgFilterEdgesModule.byTypes( Arrays.asList( CpgEdgeType.values() ), false ) )
                .add( CpgFilterEdgesModule.byTypes( Arrays.asList( CpgEdgeType.ABSTRACT_SYNTAX_TREE ), true ) )
                //.add( CpgFilterEdgesModule.byTypes( Arrays.asList( CpgEdgeType.DATA_FLOW_GRAPH, CpgEdgeType.EVALUATION_ORDER_GRAPH ), false ) )
                .add( CpgEdgeTypeVisualizeModule.instance() )
                .add( DisplayGraphModule.instance() )
                .build();
         final Graph cpg = pipe.process( Dataset.ANIMAL );
         */


        final PipeModule<Dataset, ?, Graph> pipe = PipeBuilder.<Dataset, Graph>builder()
                .add( CpgGenerateModule.instance() )
                .add( CpgToGraphModule.instance() )
                .add( PersistToNeo4jModule.instance() )
                .build();
        final Graph cpg = pipe.process( Dataset.QUICK_UML );

        log.info( "CPG: nodes {} / edges {}", cpg.getNodeCount(), cpg.getEdgeCount() );
    }

}