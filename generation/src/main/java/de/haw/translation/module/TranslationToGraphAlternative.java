package de.haw.translation.module;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.Component;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy;
import de.haw.dataset.model.Dataset;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import de.haw.repository.model.CpgEdgeType;
import de.haw.translation.CpgConst;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Deprecated
@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class TranslationToGraphAlternative<Target> extends PipeModule<TranslationResult, Graph, Target> {

    @Override
    protected Graph processImpl( final TranslationResult translationResult, final PipeContext ctx ) {

        final GraphService graphService = GraphService.instance();
        final Graph graph = graphService.getEmptyGraph();

        for ( final Component component : translationResult.getComponents() ) {
            this.traverseAST( graph, component, ctx );
        }

        return graph;
    }

    private void traverseAST( final Graph graph, final Node start, final PipeContext ctx ) {

        final SubgraphWalker.IterativeGraphWalker walkerDFG = new SubgraphWalker.IterativeGraphWalker();
        walkerDFG.setStrategy( Strategy.INSTANCE::DFG_FORWARD );
        walkerDFG.registerOnNodeVisit( ( node, parent ) -> {
            this.addNode( graph, node, ctx );
            node.getNextDFG().forEach( next -> {
                this.addEdge( graph, node, next, CpgEdgeType.DFG, ctx );
            } );
            return null;
        } );

        final SubgraphWalker.IterativeGraphWalker walkerPDG = new SubgraphWalker.IterativeGraphWalker();
        walkerPDG.setStrategy( node -> node.getNextPDG().iterator() );
        walkerPDG.registerOnNodeVisit( ( node, parent ) -> {
            this.addNode( graph, node, ctx );
            node.getNextPDG().forEach( next -> {
                this.addEdge( graph, node, next, CpgEdgeType.PDG, ctx );
            } );
            return null;
        } );

        final SubgraphWalker.IterativeGraphWalker walkerEOG = new SubgraphWalker.IterativeGraphWalker();
        walkerEOG.setStrategy( Strategy.INSTANCE::EOG_FORWARD ); // TODO maybe get property edges ???
        walkerEOG.registerOnNodeVisit( ( node, parent ) -> {
            this.addNode( graph, node, ctx );
            node.getNextEOG().forEach( next -> {
                this.addEdge( graph, node, next, CpgEdgeType.EOG, ctx );
            } );
            return null;
        } );

        final SubgraphWalker.IterativeGraphWalker walkerCDG = new SubgraphWalker.IterativeGraphWalker();
        walkerCDG.setStrategy( node -> node.getNextCDG().iterator() ); // TODO maybe get property edges ???
        walkerCDG.registerOnNodeVisit( ( node, parent ) -> {
            this.addNode( graph, node, ctx );
            node.getNextCDG().forEach( next -> {
                this.addEdge( graph, node, next, CpgEdgeType.CDG, ctx );
            } );
            return null;
        } );

        final SubgraphWalker.IterativeGraphWalker walkerAST = new SubgraphWalker.IterativeGraphWalker();
        walkerAST.setStrategy( Strategy.INSTANCE::AST_FORWARD );
        walkerAST.registerOnNodeVisit( ( node, parent ) -> {
            this.addNode( graph, node, ctx );
            node.getAstChildren().forEach( next -> {
                this.addEdge( graph, node, next, CpgEdgeType.AST, ctx );
            } );
            node.getNextDFG().forEach( next -> {
                boolean added = this.addEdge( graph, node, next, CpgEdgeType.DFG, ctx );
                if ( !added ) {
                    walkerDFG.iterate( next );
                }
            } );
            node.getNextPDG().forEach( next -> {
                boolean added = this.addEdge( graph, node, next, CpgEdgeType.PDG, ctx );
                if ( !added ) {
                    walkerPDG.iterate( next );
                }
            } );
            node.getNextEOG().forEach( next -> {
                boolean added = this.addEdge( graph, node, next, CpgEdgeType.EOG, ctx );
                if ( !added ) {
                    walkerEOG.iterate( next );
                }
            } );
            node.getNextCDG().forEach( next -> {
                boolean added = this.addEdge( graph, node, next, CpgEdgeType.CDG, ctx );
                if ( !added ) {
                    walkerCDG.iterate( next );
                }
            } );
            return null;
        } );

        walkerAST.iterate( start );
    }

    private void addNode( final Graph graph, final Node node, final PipeContext ctx ) {
        // TODO maybe label node also by walker ???
        final Dataset dataset = ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class )
                .orElseThrow( IllegalStateException::new );
        final String nodeId = this.genId( node );
        MultiNode graphNode = ( MultiNode ) graph.getNode( nodeId );
        if ( graphNode == null ) {
            graphNode = ( MultiNode ) graph.addNode( nodeId );
        }
        graphNode.setAttribute( CpgConst.NODE_ATTR_LABELS, this.getLabels( node ) );
        graphNode.setAttribute( CpgConst.NODE_ATTR_CODE, node.getCode() );
        graphNode.setAttribute( CpgConst.NODE_ATTR_DATASET, dataset.getName() );
    }

    private boolean addEdge(
            final Graph graph, final Node source, final Node target, final CpgEdgeType edgeType,
            final PipeContext ctx ) {

        final Dataset dataset = ctx.get( PipeContext.CPG_DATASET_KEY, Dataset.class )
                .orElseThrow( IllegalStateException::new );

        this.addNode( graph, target, ctx );
        final String edgeId = this.genId();
        final String sourceId = this.genId( source );
        final String targetId = this.genId( target );

        // unique edge pass TODO needed?
        if ( graph.getNode( sourceId ) != null && graph.getNode( sourceId ).getEdgeToward( targetId ) != null ) {
            return false;
        }

        final Edge graphEdge = graph.addEdge( edgeId, sourceId, targetId, true );
        graphEdge.setAttribute( CpgConst.EDGE_ATTR_LABEL, edgeType.name() );
        graphEdge.setAttribute( CpgConst.EDGE_ATTR_TYPE, edgeType.name() );
        graphEdge.setAttribute( CpgConst.EDGE_ATTR_DATASET, dataset.getName() );
        return true;
    }

    private List<String> getLabels( final Node node ) {
        final List<String> labels = new ArrayList<>();
        Class<?> currentClass = node.getClass();
        while ( currentClass.getSuperclass() != null ) {
            labels.add( currentClass.getSimpleName() );
            currentClass = currentClass.getSuperclass();
        }
        return labels;
    }

    private String genId( final Node node ) {
        return String.valueOf( System.identityHashCode( node ) );
    }

    private String genId() {
        return String.valueOf( Math.abs( UUID.randomUUID().getMostSignificantBits() ) );
    }

}
