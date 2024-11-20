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

        private final T message;

        private final int depth;

    }

    private final Set<String> visitedNodes = new HashSet<>();

    public void traverse( final Node startNode ) {
        this.traverse( startNode, -1 );
    }

    public void traverse( final Node startNode, final int maxDepth ) {

        final Deque<ProcessData<T>> queue = new ArrayDeque<>();
        queue.push( ProcessData.of( startNode, null, null, 0 ) );

        while ( !queue.isEmpty() ) {

            final ProcessData<T> processData = queue.pop();
            final Node node = processData.getNode();
            final Node parent = processData.getParent();
            final T inputData = processData.getMessage();
            final int processDepth = processData.getDepth();

            if ( maxDepth >= 0 && processDepth >= maxDepth ) {
                continue;
            }

            this.visitedNodes.add( node.getId() );
            final OutputData<T> output = this.process( node, parent, inputData );
            if( !output.isProceed() ) {
                continue;
            }

            this.next( node ).forEach( edge -> {
                if ( !this.visitedNodes.contains( edge.getTargetNode().getId() ) ) {
                    queue.push( ProcessData.of( edge.getTargetNode(), node, output.getMessage(), processDepth + 1 ) );
                }
            } );
        }

    }

    protected abstract OutputData<T> process( final Node node, final Node parent, final T message );

    protected abstract List<Edge> next( final Node node );

}
