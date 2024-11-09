package backend.core.model;

import backend.core.values.PieceType;
import lombok.Data;
import math.Vector2I;

@Data
public class Move {

    private final Vector2I from;

    private final Vector2I to;

    private final PieceType promoteTo;

    public Move( Vector2I from, Vector2I to ) {
        this.from = from;
        this.to = to;
        this.promoteTo = PieceType.QUEEN;
    }

    public Move( Vector2I from, Vector2I to, PieceType promoteTo ) {
        this.from = from;
        this.to = to;
        this.promoteTo = promoteTo;
    }

}
