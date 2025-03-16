package de.haw.processing.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum RecordInteractionType {
    EXTENDED_BY_RECORD,
    CREATES_RECORD,
    CALLS_RECORD,
    RETURNS_RECORD,
    KNOWS_RECORD;

    public static List<RecordInteractionType> all() {
        return new ArrayList<>( Arrays.asList( RecordInteractionType.values() ) );
    }

}
