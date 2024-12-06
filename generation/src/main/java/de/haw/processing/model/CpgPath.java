package de.haw.processing.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

@Data
@RequiredArgsConstructor( staticName = "of" )
public class CpgPath {

    private final Node source;

    private final Node target;

    private final Path path;

    private final double distance;

}
