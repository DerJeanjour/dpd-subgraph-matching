package de.haw.processing.module;

import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.processing.GraphService;
import de.haw.processing.traversal.GraphProcessTraverser;
import de.haw.repository.model.CpgEdgeType;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.List;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class PropagateRecordScopeModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final GraphService GS = GraphService.instance();

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        graph.nodes().forEach( node -> {

            if ( this.GS.hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ) {
                this.handleRecord( node );
                return;
            }

            final boolean alreadyPropagated = this.isAlreadyPropagated( node );
            final boolean isRecordScope = this.GS.hasLabel( node, CpgConst.NODE_LABEL_SCOPE_RECORD );
            if ( isRecordScope && !alreadyPropagated ) {
                this.handleRecordScope( node );
                return;
            }

            final boolean isScope = this.GS.hasLabel( node, CpgConst.NODE_LABEL_SCOPE );
            if ( isScope && !alreadyPropagated ) {
                this.handleIsolatedScopes( node );
            }

        } );

        return graph;
    }

    private void handleRecord( final Node node ) {
        final String recordScope = this.GS.getAttr( node, CpgConst.NODE_ATTR_NAME_FULL );
        node.setAttribute( CpgConst.NODE_ATTR_NAME_SCOPED_RECORD, recordScope );
        node.enteringEdges()
                .filter( edge -> this.GS.isType( edge, CpgEdgeType.RECORD_DECLARATION ) )
                .forEach( edge -> edge.getSourceNode()
                        .setAttribute( CpgConst.NODE_ATTR_NAME_SCOPED_RECORD, recordScope ) );
    }

    private void handleIsolatedScopes( final Node node ) {
        final String recordScope = this.GS.getAttr( node, CpgConst.NODE_ATTR_NAME_SCOPED );
        if ( StringUtils.isBlank( recordScope ) ) {
            return;
        }
        if ( recordScope.toLowerCase().equals( recordScope ) ) {
            return;
        }
        if ( recordScope.equals( this.GS.getAttr( node, CpgConst.NODE_ATTR_NAME_FULL ) ) ) {
            return;
        }
        node.setAttribute( CpgConst.NODE_ATTR_NAME_SCOPED_RECORD, recordScope );
        node.enteringEdges()
                .filter( edge -> this.GS.isType( edge, CpgEdgeType.SCOPE ) )
                .map( Edge::getSourceNode )
                .filter( source -> !isAlreadyPropagated( source ) )
                .forEach( source -> source.setAttribute( CpgConst.NODE_ATTR_NAME_SCOPED_RECORD, recordScope ) );

    }

    private boolean isAlreadyPropagated( final Node node ) {
        return this.GS.hasLabel( node, CpgConst.NODE_ATTR_NAME_SCOPED_RECORD );
    }

    private void handleRecordScope( final Node node ) {
        PropagateRecordScope.of().traverse( node );
    }

    @Slf4j
    @RequiredArgsConstructor( staticName = "of" )
    private static class PropagateRecordScope extends GraphProcessTraverser<String> {

        private final GraphService GS = GraphService.instance();

        @Override
        protected OutputData<String> process( final Node node, final String message, final TraversalContext ctx ) {

            String tempRecordScope = message;

            if ( this.GS.hasLabel( node, CpgConst.NODE_LABEL_SCOPE_RECORD ) ) {
                tempRecordScope = this.GS.getAttr( node, CpgConst.NODE_ATTR_NAME_FULL );
            }

            if ( StringUtils.isBlank( tempRecordScope ) ) {
                return OutputData.of( null, false );
            }


            final String recordScope = tempRecordScope;
            node.setAttribute( CpgConst.NODE_ATTR_NAME_SCOPED_RECORD, recordScope );
            node.enteringEdges()
                    .filter( edge -> !this.GS.isType( edge, CpgEdgeType.PARENT ) )
                    .forEach( edge -> edge.getSourceNode()
                            .setAttribute( CpgConst.NODE_ATTR_NAME_SCOPED_RECORD, recordScope ) );

            return OutputData.of( tempRecordScope, true );
        }

        @Override
        protected List<Edge> next( final Node node ) {
            return node.enteringEdges().filter( edge -> this.GS.isType( edge, CpgEdgeType.PARENT ) ).toList();
        }
    }

}
