package backend.game.modulebased;

import backend.core.model.Piece;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import lombok.Getter;
import lombok.Setter;

public class PieceMB extends Piece {

    @Getter
    @Setter
    private String id;

    public PieceMB( PieceType type, TeamColor team ) {
        super( type, team );
    }

    public PieceMB clone() {
        PieceMB piece = new PieceMB( this.type, this.team );
        piece.setAlive( this.alive );
        piece.setId( this.id );
        return piece;
    }
}
