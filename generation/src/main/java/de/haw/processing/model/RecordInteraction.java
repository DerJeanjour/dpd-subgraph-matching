package de.haw.processing.model;

import de.haw.misc.utils.PathUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Node;

@Data
@RequiredArgsConstructor( staticName = "of" )
public class RecordInteraction {

    private final RecordInteractionType type;

    private final Node source;

    private final Node target;

    private final CpgPath path;

    /**
     * @return <code>true</code> if the source and target of the interaction is reversed to the path it is based on.
     */
    public boolean isReversed() {
        final Node pathTarget = PathUtils.getLastNode( this.path.getPath() );
        if ( pathTarget == null || StringUtils.isBlank( pathTarget.getId() ) ) {
            return false;
        }
        return !this.target.getId().equals( pathTarget.getId() );

    }

}
