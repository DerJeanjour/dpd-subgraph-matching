package de.haw.processing.model;

import de.haw.misc.utils.PathUtils;
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

    @Override
    public String toString() {
        return "CpgPath{ path=" + PathUtils.pathToString( this.path, true ) + " distance=" + distance + " }";
    }
}
