package backend.core.values;

import math.Vector2I;

import java.util.Arrays;
import java.util.List;

public enum Dir {

    UP( Vector2I.UNIT_Y ),
    RIGHT( Vector2I.UNIT_X ),
    DOWN( Vector2I.UNIT_Y.negative() ),
    LEFT( Vector2I.UNIT_X.negative() ),
    UP_RIGHT( UP.vector.add( RIGHT.vector ) ),
    DOWN_RIGHT( DOWN.vector.add( RIGHT.vector ) ),
    UP_LEFT( UP.vector.add( LEFT.vector ) ),
    DOWN_LEFT( DOWN.vector.add( LEFT.vector ) );

    public Vector2I vector;

    Dir( Vector2I vector ) {
        this.vector = vector;
    }

    public static List<Dir> baseDirs() {
        return Arrays.asList(
                UP,
                RIGHT,
                DOWN,
                LEFT
        );
    }

    public static List<Dir> diagonalDirs() {
        return Arrays.asList(
                UP_RIGHT,
                UP_LEFT,
                DOWN_RIGHT,
                DOWN_LEFT
        );
    }

}
