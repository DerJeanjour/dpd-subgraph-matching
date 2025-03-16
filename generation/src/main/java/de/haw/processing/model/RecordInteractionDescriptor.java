package de.haw.processing.model;

import de.haw.repository.model.CpgEdgeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor( staticName = "of" )
public class RecordInteractionDescriptor {

    // @formatter:off
    public static final RecordInteractionDescriptor EXTENDED_BY_INTERACTION = RecordInteractionDescriptor.of(
            0,
            RecordInteractionType.EXTENDED_BY_RECORD,
            Arrays.asList(
                    RecordInteractionType.EXTENDED_BY_RECORD ),
            Arrays.asList( CpgEdgeType.SUPER_TYPE_DECLARATIONS ),
            true
    );

    public static final RecordInteractionDescriptor CREATES_INTERACTION = RecordInteractionDescriptor.of(
            1,
            RecordInteractionType.CREATES_RECORD,
            Arrays.asList(
                    RecordInteractionType.RETURNS_RECORD,
                    RecordInteractionType.CREATES_RECORD,
                    RecordInteractionType.EXTENDED_BY_RECORD ),
            Arrays.asList( CpgEdgeType.INSTANTIATES ),
            false
    );

    public static final RecordInteractionDescriptor CALLS_INTERACTION = RecordInteractionDescriptor.of(
            2,
            RecordInteractionType.CALLS_RECORD,
            Arrays.asList(
                    RecordInteractionType.CALLS_RECORD,
                    RecordInteractionType.RETURNS_RECORD,
                    RecordInteractionType.CREATES_RECORD,
                    RecordInteractionType.EXTENDED_BY_RECORD ),
            Arrays.asList( CpgEdgeType.INVOKES ),
            false
    );

    public static final RecordInteractionDescriptor RETURNS_INTERACTION = RecordInteractionDescriptor.of(
            3,
            RecordInteractionType.RETURNS_RECORD,
            Arrays.asList(
                    RecordInteractionType.CALLS_RECORD,
                    RecordInteractionType.RETURNS_RECORD,
                    RecordInteractionType.CREATES_RECORD,
                    RecordInteractionType.EXTENDED_BY_RECORD ),
            Arrays.asList( CpgEdgeType.RETURN_TYPES ),
            false
    );

    public static final RecordInteractionDescriptor KNOWS_INTERACTION = RecordInteractionDescriptor.of(
            4,
            RecordInteractionType.KNOWS_RECORD,
            Arrays.asList( RecordInteractionType.values() ),
            Collections.emptyList(),
            false
    );

    public static final List<RecordInteractionDescriptor> ALL = Arrays.asList(
            EXTENDED_BY_INTERACTION,
            CREATES_INTERACTION,
            CALLS_INTERACTION,
            RETURNS_INTERACTION,
            KNOWS_INTERACTION
    );
    // @formatter:on

    private final int order;

    private final RecordInteractionType type;

    private final List<RecordInteractionType> allowedSiblings;

    private final List<CpgEdgeType> edgePivots;

    private final boolean reversedInteraction;

    public static RecordInteractionDescriptor get( final RecordInteractionType type ) {
        return switch ( type ) {
            case EXTENDED_BY_RECORD -> EXTENDED_BY_INTERACTION;
            case CREATES_RECORD -> CREATES_INTERACTION;
            case CALLS_RECORD -> CALLS_INTERACTION;
            case RETURNS_RECORD -> RETURNS_INTERACTION;
            case KNOWS_RECORD -> KNOWS_INTERACTION;
        };
    }

    public boolean hasEdgePivot( final CpgEdgeType type ) {
        return this.edgePivots.contains( type );
    }

    public boolean hasEdgePivot( final List<CpgEdgeType> types ) {
        return types.stream().anyMatch( this::hasEdgePivot );
    }

}
