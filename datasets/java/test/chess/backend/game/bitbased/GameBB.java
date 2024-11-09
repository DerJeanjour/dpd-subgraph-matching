package backend.game.bitbased;

import backend.core.model.Move;
import backend.core.model.Piece;
import backend.core.model.Team;
import backend.core.model.Validation;
import backend.core.notation.ChessNotation;
import backend.core.values.ActionType;
import backend.core.values.GameState;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import backend.game.Game;
import backend.game.GameConfig;
import math.Vector2I;

import java.util.*;
import java.util.stream.Collectors;

public class GameBB extends Game {

    private final BitBoard board;

    private final boolean canLog;

    public GameBB( final GameConfig config ) {
        super( config );
        board = new BitBoard( config );
        this.canLog = false;
    }

    public GameBB( final GameConfig config, final boolean canLog ) {
        super( config );
        board = new BitBoard( config );
        this.canLog = canLog;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setGame( String notation, ChessNotation notationProcessor ) {
        final Game game = notationProcessor.read( notation );
        // TODO
    }

    @Override
    public boolean makeMove( Move move ) {
        if( !this.isLegal( move ) ) {
            return false;
        }
        if( this.hasPiece( move.getTo() ) ) {
            this.board.removePiece( move.getTo() );
        }
        final Piece piece = this.getPiece( move.getFrom() );
        this.board.removePiece( move.getFrom() );
        this.board.setPiece( move.getTo(), piece );
        this.board.logPieces();
        return true;
    }

    @Override
    public void undoLastMove() {

    }

    @Override
    public List<Validation> validate( Vector2I p ) {
        return this.board.getLegal( p ).stream()
                .map( to -> {
                    final Validation validation = new Validation( new Move( p, to ) );
                    validation.getActions().add( ActionType.MOVE );
                    return validation;
                } )
                .collect( Collectors.toList());
    }

    @Override
    public boolean isLegal( Move move ) {
        if( this.isOutOfBounds( move.getTo() ) ) {
            return false;
        }
        final Piece piece = this.getPiece( move.getFrom() );
        if( piece == null ) {
            return false;
        }
        return this.validate( move.getFrom() ).stream()
                .anyMatch( v -> v.getMove().getTo().equals( move.getTo() ) && v.isLegal() );
    }

    @Override
    public int getBoardSize() {
        return 8;
    }

    @Override
    public List<Move> getPossibleMoves( TeamColor color ) {
        return null;
    }

    @Override
    public List<Move> getPossibleMoves( Piece piece ) {
        return null;
    }

    @Override
    public List<Validation> getPossibleValidations( TeamColor color ) {
        return null;
    }

    @Override
    public List<Validation> getPossibleValidations( Piece piece ) {
        return null;
    }

    @Override
    public boolean isOnMove( TeamColor color ) {
        return super.onMove.equals( color );
    }

    @Override
    public boolean isCheckFor( TeamColor team ) {
        return false;
    }

    @Override
    public boolean hasLegalMovesLeft( TeamColor team ) {
        return false;
    }

    @Override
    public boolean isCheckmateFor( TeamColor team ) {
        return false;
    }

    @Override
    public boolean isStalemateFor( TeamColor team ) {
        return false;
    }

    @Override
    public boolean isFinished() {
        return EnumSet.of( GameState.TIE, GameState.WHITE_WON, GameState.BLACK_WON ).contains( this.state );
    }

    @Override
    public boolean isType( Vector2I p, PieceType type ) {
        if( type == null ) {
            return false;
        }
        return type.equals( this.getType( p ) );
    }

    @Override
    public PieceType getType( Vector2I p ) {
        final Optional<Piece> piece = this.board.getPiece( p );
        return piece.map( Piece::getType ).orElse( null );
    }

    @Override
    public Piece getPiece( Vector2I p ) {
        return this.board.getPiece( p ).orElse( null );
    }

    @Override
    public boolean hasPiece( Vector2I p ) {
        return this.board.getPiece( p ).isPresent();
    }

    @Override
    public Vector2I getPosition( Piece piece ) {
        // TODO
        return null;
    }

    @Override
    public boolean isTeam( Vector2I p, TeamColor team ) {
        if( team == null ) {
            return false;
        }
        return team.equals( this.getTeam( p ) );
    }

    @Override
    public TeamColor getTeam( Vector2I p ) {
        final Optional<Piece> piece = this.board.getPiece( p );
        return piece.map( Piece::getTeam ).orElse( null );
    }

    @Override
    public Team getTeam( TeamColor color ) {
        return null;
    }

    @Override
    public TeamColor getEnemy( Vector2I p ) {
        return null;
    }

    @Override
    public TeamColor getEnemy( TeamColor team ) {
        return null;
    }

    @Override
    public boolean areEnemies( Vector2I pA, Vector2I pB ) {
        return false;
    }

    @Override
    public boolean isAttacked( Vector2I p ) {
        return false;
    }

    @Override
    public boolean isPined( Vector2I p ) {
        return false;
    }
}
