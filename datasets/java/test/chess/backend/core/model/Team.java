package backend.core.model;

import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public abstract class Team {

    @Getter
    protected final TeamColor color;

    public abstract Piece getKing();

    public abstract List<Piece> getAll();

    public abstract List<Piece> getAlive();

    public abstract List<Piece> getPiecesByType( PieceType type );

    public abstract List<Piece> getPiecesByType( PieceType type, boolean alive );

}
