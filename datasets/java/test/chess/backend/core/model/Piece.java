package backend.core.model;

import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import lombok.Data;

@Data
public class Piece {

    protected PieceType type;

    protected final TeamColor team;

    protected boolean alive;

    public Piece( PieceType type, TeamColor team ) {
        this.type = type;
        this.team = team;
        this.alive = true;
    }

    public boolean isType( PieceType type ) {
        return this.type.equals( type );
    }

    public boolean isTeam( TeamColor team ) {
        return this.team.equals( team );
    }

    @Override
    public String toString() {
        return "[" + type.name() + "/" + team.name() + "/" + ( alive ? "alive" : "dead" ) + "]";
    }

}
