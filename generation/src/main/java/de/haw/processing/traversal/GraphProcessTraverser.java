package de.haw.processing.traversal;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

import java.util.*;

public abstract class GraphProcessTraverser<T> {

    @Data
    @RequiredArgsConstructor( staticName = "of" )
    protected static class OutputData<T> {

        private final T message;

        private final boolean proceed;

    }

    @Data
    @RequiredArgsConstructor( staticName = "of" )
    protected static class ProcessData<T> {

        private final Node node;

        private final Node parent;

        private final Edge edge;

        private final T message;

        private final int depth;

    }

    @Data
    @RequiredArgsConstructor( staticName = "of" )
    protected static class TraversalContext {

        private final Edge edge;

        private final Node parent;

        private final int depth;

    }

    private final Set<String> visitedEdges = new HashSet<>();

    public void traverse( final Node startNode ) {
        this.traverse( startNode, -1 );
    }

    public void traverse( final Node startNode, final int maxDepth ) {

        final Deque<ProcessData<T>> queue = new ArrayDeque<>();
        this.next( startNode )
                .forEach( next -> queue.push(
                        ProcessData.of( startNode, next.getOpposite( startNode ), next, null, 0 ) ) );

        while ( !queue.isEmpty() ) {

            final ProcessData<T> processData = queue.pop();
            final Node node = processData.getNode();
            final T inputData = processData.getMessage();
            final TraversalContext ctx = TraversalContext.of(
                    processData.getEdge(), processData.getParent(), processData.getDepth() );

            if ( maxDepth >= 0 && ctx.getDepth() >= maxDepth ) {
                continue;
            }

            final OutputData<T> output = this.process( node, inputData, ctx );
            this.visitedEdges.add( ctx.getEdge().getId() );
            if ( !output.isProceed() ) {
                continue;
            }

            this.next( node ).forEach( next -> {
                if ( !this.visitedEdges.contains( next.getId() ) ) {
                    queue.push( ProcessData.of( next.getOpposite( node ), node, next, output.getMessage(),
                            ctx.getDepth() + 1 ) );
                }
            } );
        }

    }

    protected abstract OutputData<T> process( final Node node, final T message, final TraversalContext ctx );

    protected abstract List<Edge> next( final Node node );

}
