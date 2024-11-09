package backend.game.modulebased;

import backend.core.model.Piece;
import backend.core.model.Team;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamMB extends Team {

    @Setter
    private Map<String, PieceMB> pieces;

    public TeamMB( TeamColor color ) {
        super( color );
        this.pieces = new HashMap<>();
    }

    public String registerPiece( PieceMB piece ) {
        if ( !piece.isTeam( this.color ) ) {
            return null;
        }
        List<Piece> piecesOfType = getPiecesByType( piece.getType() );
        String id = color.equals( TeamColor.WHITE ) ? "W" : "B";
        id += "_" + piece.getType() + "_" + piecesOfType.size();
        piece.setId( id );
        this.pieces.put( id, piece );
        return id;
    }

    public PieceMB getById( String id ) {
        return this.pieces.get( id );
    }

    public List<Piece> getAll() {
        return this.pieces.values().stream().collect( Collectors.toList() );
    }

    public List<Piece> getAlive() {
        return getAll().stream().filter( Piece::isAlive ).collect( Collectors.toList() );
    }

    public Piece getKing() {
        return getAll().stream().filter( p -> PieceType.KING.equals( p.getType() ) ).findFirst().get();
    }

    public List<Piece> getPiecesByType( PieceType type ) {
        return getAll().stream().filter( p -> p.isType( type ) ).collect( Collectors.toList() );
    }

    public List<Piece> getPiecesByType( PieceType type, boolean alive ) {
        return getPiecesByType( type ).stream().filter( p -> p.isAlive() == alive ).collect( Collectors.toList() );
    }

    public TeamMB clone() {
        TeamMB team = new TeamMB( this.color );
        Map<String, PieceMB> pieces = new HashMap<>();
        for ( Map.Entry<String, PieceMB> entry : this.pieces.entrySet() ) {
            pieces.put( entry.getKey(), entry.getValue().clone() );
        }
        team.setPieces( pieces );
        return team;
    }

}
