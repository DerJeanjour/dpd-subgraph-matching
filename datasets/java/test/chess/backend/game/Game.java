package backend.game;

import backend.core.model.*;
import backend.core.notation.ChessNotation;
import backend.core.values.ActionType;
import backend.core.values.GameState;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import backend.game.bitbased.GameBB;
import backend.game.modulebased.GameMB;
import lombok.Getter;
import lombok.Setter;
import math.Vector2I;
import util.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Game {

    public static Game getInstance( final GameConfig config ) {
        return new GameBB( config );
    }

    public static Game getTestInstance( final GameConfig config ) {
        return new GameMB( config );
    }

    public static Game getInstance( final GameConfig config, final boolean canLog ) {
        return new GameBB( config, canLog );
    }

    @Getter
    protected final GameConfig config;

    @Getter
    protected GameState state;

    @Getter
    protected TeamColor onMove;

    @Setter
    @Getter
    protected boolean whiteCanCastleKing;

    @Setter
    @Getter
    protected boolean whiteCanCastleQueen;

    @Setter
    @Getter
    protected boolean blackCanCastleKing;

    @Setter
    @Getter
    protected boolean blackCanCastleQueen;

    @Getter
    protected int moveNumber;

    @Getter
    protected int halfMoveRuleCount;

    @Setter
    @Getter
    protected Vector2I auPassantPosition;

    @Getter
    protected List<MoveHistory> history;

    private List<GameListener> listeners;

    public Game( GameConfig config ) {
        this.config = config;
        this.listeners = new ArrayList<>();
        this.resetStates();
    }

    /**
     * core
     */

    public abstract void reset();

    public abstract void setGame( String notation, ChessNotation notationProcessor );

    public abstract boolean makeMove( Move move );

    public abstract void undoLastMove();

    public abstract List<Validation> validate( Vector2I p );

    public abstract boolean isLegal( Move move );

    public abstract int getBoardSize();

    public abstract List<Move> getPossibleMoves( TeamColor color );

    public abstract List<Move> getPossibleMoves( Piece piece );

    public abstract List<Validation> getPossibleValidations( TeamColor color );

    public abstract List<Validation> getPossibleValidations( Piece piece );

    /**
     * state
     */

    public abstract boolean isOnMove( TeamColor color );

    public abstract boolean isCheckFor( TeamColor team );

    public abstract boolean hasLegalMovesLeft( TeamColor team );

    public abstract boolean isCheckmateFor( TeamColor team );

    public abstract boolean isStalemateFor( TeamColor team );

    public abstract boolean isFinished();

    /**
     * convenience
     */

    public abstract boolean isType( Vector2I p, PieceType type );

    public abstract PieceType getType( Vector2I p );

    public abstract Piece getPiece( Vector2I p );

    public abstract boolean hasPiece( Vector2I p );

    public abstract Vector2I getPosition( Piece piece );

    public abstract boolean isTeam( Vector2I p, TeamColor team );

    public abstract TeamColor getTeam( Vector2I p );

    public abstract Team getTeam( TeamColor color );

    public abstract TeamColor getEnemy( Vector2I p );

    public abstract TeamColor getEnemy( TeamColor team );

    public abstract boolean areEnemies( Vector2I pA, Vector2I pB );

    public abstract boolean isAttacked( Vector2I p );

    public abstract boolean isPined( Vector2I p );

    /**
     * listener
     */

    public void addListener( GameListener listener ) {
        this.listeners.add( listener );
    }

    public void emitEvent() {
        this.listeners.forEach( l -> l.gameUpdated( this ) );
    }

    /**
     * general
     */

    public MoveHistory getLastMove() {
        if ( this.history.isEmpty() ) {
            return null;
        }
        return this.history.get( this.history.size() - 1 );
    }

    protected void addHistory( Set<ActionType> actions, Move move ) {
        this.history.add( new MoveHistory(
                this.moveNumber,
                actions,
                getTeam( move.getTo() ),
                getType( move.getTo() ),
                move
        ) );
    }

    protected void switchTeam() {
        this.onMove = isOnMove( TeamColor.WHITE ) ? TeamColor.BLACK : TeamColor.WHITE;
        this.state = isOnMove( TeamColor.BLACK ) ? GameState.BLACK_TO_MOVE : GameState.WHITE_TO_MOVE;
    }

    protected void incrementMove( Validation validation ) {
        if ( isFinished() ) {
            return;
        }
        if ( isOnMove( this.config.getOnMove() ) ) {
            this.moveNumber++;
        }
        if ( !validation.getActions().contains( ActionType.CAPTURE ) &&
                !validation.getActions().contains( ActionType.CAPTURE_AU_PASSANT ) &&
                !this.isType( validation.getMove().getTo(), PieceType.PAWN ) ) {
            this.halfMoveRuleCount++;
        } else {
            this.halfMoveRuleCount = 0;
        }
    }

    public boolean isOutOfBounds( Vector2I p ) {
        if ( p == null ) {
            return true;
        }
        return MathUtil.isOutOfBounds( p.x, this.getBoardSize() )
                || MathUtil.isOutOfBounds( p.y, this.getBoardSize() );
    }

    public boolean isLegal( Map<Vector2I, Validation> validation, Vector2I p ) {
        return hasAction( validation, p ) && validation.get( p ).isLegal();
    }

    public boolean hasAction( Map<Vector2I, Validation> validation, Vector2I p ) {
        return validation.get( p ) != null && validation.get( p ).hasAction();
    }

    public boolean hasAction( Map<Vector2I, Validation> validation, Vector2I p, ActionType type ) {
        return validation.get( p ) != null && validation.get( p ).hasAction( type );
    }

    protected void resetStates() {
        this.onMove = this.config.getOnMove();
        this.whiteCanCastleKing = this.config.isWhiteCanCastleKing();
        this.whiteCanCastleQueen = this.config.isWhiteCanCastleQueen();
        this.blackCanCastleKing = this.config.isBlackCanCastleKing();
        this.blackCanCastleQueen = this.config.isBlackCanCastleQueen();
        this.auPassantPosition = null;
        this.halfMoveRuleCount = this.config.getHalfMoveRuleCount();
        this.moveNumber = this.config.getMoveNumber();
        this.history = new ArrayList<>();
        this.state = this.isOnMove( TeamColor.WHITE ) ? GameState.WHITE_TO_MOVE : GameState.BLACK_TO_MOVE;
    }

}
