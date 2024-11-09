package backend.game.modulebased;

import backend.core.exception.IllegalMoveException;
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
import backend.game.MoveGenerator;
import backend.game.modulebased.validator.RuleType;
import backend.game.modulebased.validator.RuleValidator;
import backend.game.modulebased.validator.ValidationMB;
import lombok.Getter;
import math.Vector2I;
import misc.Log;
import util.CollectionUtil;
import util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class GameMB extends Game {

    @Getter
    private TeamMB white;

    @Getter
    private TeamMB black;

    @Getter
    private Map<Vector2I, String> positions;

    @Getter
    private Map<String, Vector2I> piecePositions;

    @Getter
    private RuleValidator ruleValidator;

    @Getter
    private final boolean canLog;

    @Getter
    private GameMB prev;

    @Getter
    private Set<Vector2I> attacked;

    @Getter
    private List<List<Vector2I>> pined;

    public GameMB( final GameConfig config ) {
        super( config );
        this.canLog = false;
        reset();
    }

    public GameMB( final GameConfig config, boolean canLog ) {
        super( config );
        this.canLog = canLog;
        reset();
    }

    @Override
    public void reset() {
        this.white = new TeamMB( TeamColor.WHITE );
        this.black = new TeamMB( TeamColor.BLACK );
        this.ruleValidator = new RuleValidator( this, Arrays.asList( RuleType.values() ) );
        this.resetStates();
        this.initPositions();
        this.prev = null;
        this.attacked = MoveGenerator.generateAttackedPositionsBy( this, getEnemy( this.onMove ) );
        this.pined = MoveGenerator.generatePinedPositionsBy( this, getEnemy( this.onMove ) );
        this.emitEvent();
    }

    @Override
    public void setGame( String notation, ChessNotation notationProcessor ) {
        GameMB game = ( GameMB ) notationProcessor.read( notation );
        this.setAll( game );
    }

    @Override
    public void undoLastMove() {
        if ( this.prev == null ) {
            return;
        }
        this.setAll( this.prev );
    }

    @Override
    public List<Validation> validate( Vector2I p ) {
        List<Validation> validations = new ArrayList<>();
        this.ruleValidator.validate( p ).values().forEach( v -> validations.addAll( v ) );
        return validations;
    }

    @Override
    public boolean isLegal( Move move ) {
        return this.ruleValidator.validate( move ).get( 0 ).isLegal();
    }

    @Override
    public synchronized boolean makeMove( Move move ) {

        if ( move == null || move.getFrom() == null || move.getTo() == null ) {
            return false;
        }

        Vector2I from = move.getFrom();
        Vector2I to = move.getTo();

        try {

            if ( getPiece( from ) == null ) {
                return false;
            }

            List<ValidationMB> validatedPositions = this.ruleValidator.validate( move );

            ValidationMB validatedPosition = validatedPositions.get( 0 );
            if ( validatedPositions.size() > 1 ) {
                ActionType promoteAction = ActionType.PROMOTING_QUEEN;
                switch ( move.getPromoteTo() ) {
                    case QUEEN -> promoteAction = ActionType.PROMOTING_QUEEN;
                    case ROOK -> promoteAction = ActionType.PROMOTING_ROOK;
                    case BISHOP -> promoteAction = ActionType.PROMOTING_BISHOP;
                    case KNIGHT -> promoteAction = ActionType.PROMOTING_KNIGHT;
                }
                for ( ValidationMB v : validatedPositions ) {
                    if ( v.getActions().contains( promoteAction ) ) {
                        validatedPosition = v;
                    }
                }
            }

            if ( validatedPosition.isLegal() ) {

                this.prev = this.clone();
                movePiece( from, to );
                this.ruleValidator.applyAdditionalActions( validatedPosition.getActions(), from, to );

                handleAuPassantPosition();
                switchTeam();

                this.attacked = MoveGenerator.generateAttackedPositionsBy( this, getEnemy( this.onMove ) );
                this.pined = MoveGenerator.generatePinedPositionsBy( this, getEnemy( this.onMove ) );
                this.ruleValidator.postValidate( validatedPosition );

                log( "On {}s {}. move: {} {}->{} with actions {}",
                        this.getTeam( to ),
                        this.moveNumber,
                        getType( to ),
                        from,
                        to,
                        validatedPosition.getActions() );
                addHistory( validatedPosition.getActions(), move );

                checkFinished( validatedPosition.getActions() );
                incrementMove( validatedPosition );

                this.emitEvent();
                return true;
            }
            return false;

        } catch ( Exception e ) {
            throw new IllegalMoveException( this, from, to, e );
        }

    }

    public void movePiece( Vector2I from, Vector2I to ) {
        PieceMB piece = ( PieceMB ) getPiece( from );
        if ( piece == null ) {
            return;
        }
        removePiece( to );
        this.positions.put( from, "" );
        this.positions.put( to, piece.getId() );

        this.piecePositions.put( piece.getId(), to );
    }

    public void removePiece( Vector2I pos ) {
        Piece piece = getPiece( pos );
        this.positions.put( pos, "" );
        if ( piece != null ) {
            piece.setAlive( false );
            this.piecePositions.remove( getPieceMb( piece ).getId() );
        }
    }

    @Override
    public boolean isOnMove( TeamColor color ) {
        return this.onMove.equals( color );
    }

    @Override
    public boolean isCheckFor( TeamColor team ) {

        final Piece king = this.getTeam( team ).getKing();
        if ( !king.isAlive() ) {
            return false;
        }

        return MoveGenerator.generateAttackedPositionsBy( this, getEnemy( team ) ).contains( getPosition( king ) );
    }

    @Override
    public boolean hasLegalMovesLeft( TeamColor color ) {
        for ( Vector2I from : this.getAllAlivePositionsOf( color ) ) {
            Set<Vector2I> positions = MoveGenerator.generateAllPossibleMoves( this, from );
            for ( Vector2I to : positions ) {
                if ( this.isLegal( new Move( from, to ) ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isCheckmateFor( TeamColor color ) {
        return isCheckFor( color ) && !hasLegalMovesLeft( color );
    }

    @Override
    public boolean isStalemateFor( TeamColor color ) {

        // check if each team only has their kings left
        Team white = this.getTeam( TeamColor.WHITE );
        Team black = this.getTeam( TeamColor.BLACK );
        if ( white.getAlive().size() == 1 && black.getAlive().size() == 1 ) {
            return true;
        }

        return !isCheckFor( color ) && !hasLegalMovesLeft( color );
    }

    private void checkFinished( Set<ActionType> actions ) {

        if ( actions.contains( ActionType.CHECKMATE ) ) {
            this.state = isOnMove( TeamColor.WHITE )
                    ? GameState.BLACK_WON
                    : GameState.WHITE_WON;
        }

        if ( actions.contains( ActionType.STALEMATE ) || this.halfMoveRuleCount >= 50 ) {
            this.state = GameState.TIE;
        }

    }

    @Override
    public boolean isFinished() {
        return EnumSet.of( GameState.TIE, GameState.WHITE_WON, GameState.BLACK_WON ).contains( this.state );
    }

    private void handleAuPassantPosition() {
        if ( this.auPassantPosition == null ) {
            return;
        }
        Piece piece = getPiece( this.auPassantPosition );
        if ( piece == null || !piece.getTeam().equals( this.onMove ) ) {
            this.auPassantPosition = null;
        }
    }

    public List<Vector2I> getAllAlivePositionsOf( TeamColor color ) {
        Team team = getTeam( color );
        if ( team == null ) {
            return Collections.emptyList();
        }
        return team.getAlive().stream()
                .map( piece -> this.getPosition( piece ) )
                .collect( Collectors.toList() );
    }

    public boolean isType( Vector2I p, PieceType type ) {
        return type.equals( getType( p ) );
    }

    public PieceType getType( Vector2I p ) {
        Piece piece = getPiece( p );
        if ( piece == null ) {
            return null;
        }
        return piece.getType();
    }

    @Override
    public boolean isTeam( Vector2I p, TeamColor team ) {
        return team.equals( getTeam( p ) );
    }

    public TeamColor getTeam( Vector2I p ) {
        Piece piece = getPiece( p );
        if ( piece == null ) {
            return null;
        }
        return piece.getTeam();
    }

    public TeamColor getEnemy( Vector2I p ) {
        return TeamColor.getEnemy( getTeam( p ) );
    }

    public TeamColor getEnemy( TeamColor color ) {
        return TeamColor.getEnemy( color );
    }

    public TeamColor getEnemy() {
        return getEnemy( this.onMove );
    }

    @Override
    public boolean isAttacked( Vector2I p ) {
        if ( this.attacked == null ) {
            return false;
        }
        return this.attacked.contains( p );
    }

    @Override
    public boolean isPined( Vector2I p ) {
        return this.getPinIdx( p ) >= 0;
    }

    public int getPinIdx( Vector2I p ) {
        for ( int i = 0; i < this.pined.size(); i++ ) {
            List<Vector2I> ray = this.pined.get( i );
            if ( ray.contains( p ) ) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean areEnemies( Vector2I pA, Vector2I pB ) {
        Piece pieceA = getPiece( pA );
        Piece pieceB = getPiece( pB );
        if ( pieceA == null || pieceB == null ) {
            return false;
        }
        return !pieceA.isTeam( pieceB.getTeam() );
    }

    @Override
    public Piece getPiece( Vector2I p ) {
        String pieceId = this.positions.get( p );
        if ( StringUtil.isBlank( pieceId ) ) {
            return null;
        }
        return getPiece( this.positions.get( p ) );
    }

    @Override
    public boolean hasPiece( Vector2I p ) {
        return getPiece( p ) != null;
    }

    public Piece getPiece( String id ) {
        TeamMB team = getTeam( id );
        return team.getById( id );
    }

    @Override
    public Vector2I getPosition( Piece piece ) {
        if ( piece == null ) {
            return null;
        }
        return this.piecePositions.get( getPieceMb( piece ).getId() );
    }

    public TeamMB getTeam( String id ) {
        return id.startsWith( "W" ) ? this.white : this.black;
    }

    @Override
    public Team getTeam( TeamColor color ) {
        return color.equals( TeamColor.WHITE ) ? this.white : this.black;
    }

    private void initPositions() {
        this.white = new TeamMB( TeamColor.WHITE );
        this.black = new TeamMB( TeamColor.BLACK );
        this.piecePositions = new HashMap<>();
        this.positions = new HashMap<>();
        for ( int i = 0; i < this.getBoardSize(); i++ ) {
            for ( int j = 0; j < this.getBoardSize(); j++ ) {
                this.positions.put( new Vector2I( i, j ), "" );
            }
        }
        for ( Map.Entry<Vector2I, Piece> placement : this.config.getPlacements().entrySet() ) {
            final Vector2I position = placement.getKey();
            PieceMB piece = new PieceMB( placement.getValue().getType(), placement.getValue().getTeam() );
            TeamMB team = piece.isTeam( TeamColor.WHITE ) ? this.white : this.black;
            final String id = team.registerPiece( piece );
            this.positions.put( position, id );
            this.piecePositions.put( id, position );
        }
    }

    private PieceMB getPieceMb( Piece piece ) {
        return ( PieceMB ) piece;
    }

    @Override
    public int getBoardSize() {
        return this.config.getBoardSize();
    }

    @Override
    public List<Move> getPossibleMoves( TeamColor color ) {
        List<Move> possibleMoves = new ArrayList<>();
        List<Piece> alive = getTeam( color ).getAlive();
        alive.forEach( piece -> possibleMoves.addAll( getPossibleMoves( piece ) ) );
        return possibleMoves;
    }

    @Override
    public List<Move> getPossibleMoves( Piece piece ) {
        return getPossibleValidations( piece ).stream()
                .map( Validation::getMove )
                .collect( Collectors.toList() );
    }

    @Override
    public List<Validation> getPossibleValidations( TeamColor color ) {
        List<Validation> validations = new ArrayList<>();
        List<Piece> alive = getTeam( color ).getAlive();
        alive.forEach( piece -> validations.addAll( getPossibleValidations( piece ) ) );
        return validations;
    }

    @Override
    public List<Validation> getPossibleValidations( Piece piece ) {
        if ( !piece.isAlive() ) {
            return Collections.EMPTY_LIST;
        }
        Vector2I p = getPosition( piece );
        List<Validation> validations = this.validate( p );
        return validations.stream()
                .filter( Validation::isLegal )
                .collect( Collectors.toList() );
    }

    public GameMB clone() {
        GameMB game = new GameMB( this.config );
        game.setAll( this );
        return game;
    }

    public void setAll( GameMB game ) {
        this.white = game.getWhite().clone();
        this.black = game.getBlack().clone();
        this.positions = game.getPositions().entrySet().stream().collect( Collectors.toMap( e -> e.getKey(), e -> e.getValue() ) );
        this.piecePositions = game.getPiecePositions().entrySet().stream().collect( Collectors.toMap( e -> e.getKey(), e -> e.getValue() ) );
        this.state = game.getState();
        this.history = new ArrayList<>( game.getHistory() );
        this.prev = game.getPrev();
        this.attacked = game.getAttacked();
        this.pined = game.getPined();
        this.onMove = game.getOnMove();
        this.whiteCanCastleKing = game.isWhiteCanCastleKing();
        this.whiteCanCastleQueen = game.isWhiteCanCastleQueen();
        this.blackCanCastleKing = game.isBlackCanCastleKing();
        this.blackCanCastleQueen = game.isBlackCanCastleQueen();
        this.auPassantPosition = game.getAuPassantPosition();
        this.moveNumber = game.getMoveNumber();
        this.halfMoveRuleCount = game.getHalfMoveRuleCount();
        this.ruleValidator = game.getRuleValidator().clone( this );
        this.emitEvent();
    }

    public void log( String pattern, Object... arguments ) {
        if ( this.canLog ) {
            List<Object> argumentList = CollectionUtil.toMutableList( arguments );
            Log.info( pattern, argumentList.toArray() );
        }
    }

}
