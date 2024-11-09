package backend.game.bitbased;

import backend.core.model.Piece;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import backend.game.GameConfig;
import math.Vector2I;
import misc.Log;

import java.util.*;

public class BitBoard {

    private final long[] whitePieces;

    private long white;

    private final long[] blackPieces;

    private long black;

    private static final int BOARD_SIZE = 8;

    private static final long EMPTY_MASK = 0L;

    private static final long FULL_MASK = Long.MAX_VALUE;

    private static final long RANK_MASK = 0xFFL;

    private static final long FILE_MASK = 0x0101010101010101L;

    private static final long LL_TR_DIAGONAL_MASK = 0x8040201008040201L;

    private static final long TL_LR_DIAGONAL_MASK = 0x0102040810204080L;

    public BitBoard( final GameConfig config ) {
        if ( config == null || config.getBoardSize() != BOARD_SIZE ) {
            throw new IllegalArgumentException();
        }
        this.whitePieces = new long[]{ EMPTY_MASK, EMPTY_MASK, EMPTY_MASK, EMPTY_MASK, EMPTY_MASK, EMPTY_MASK };
        this.white = EMPTY_MASK;
        this.blackPieces = new long[]{ EMPTY_MASK, EMPTY_MASK, EMPTY_MASK, EMPTY_MASK, EMPTY_MASK, EMPTY_MASK };
        this.black = EMPTY_MASK;
        config.getPlacements().forEach( this::setPiece );

        this.logPieces();
    }

    private long posToBit( final Vector2I pos ) {
        return 1L << ( pos.y * BOARD_SIZE + pos.x );
    }

    private Vector2I bitToPos( final long bit ) {
        int index = Long.numberOfTrailingZeros( bit );
        int y = index / BOARD_SIZE;
        int x = index % BOARD_SIZE;
        return new Vector2I( x, y );
    }

    public void removePiece( final Vector2I pos ) {

        long bit = posToBit( pos );

        for ( PieceType type : PieceType.values() ) {
            this.whitePieces[type.ordinal()] &= ~bit;
            this.white &= ~bit;
            this.blackPieces[type.ordinal()] &= ~bit;
            this.black &= ~bit;
        }
    }

    public void setPiece( final Vector2I pos, final Piece piece ) {

        long bit = posToBit( pos );

        if ( TeamColor.WHITE.equals( piece.getTeam() ) ) {
            this.whitePieces[piece.getType().ordinal()] |= bit;
            this.white |= bit;
        } else {
            this.blackPieces[piece.getType().ordinal()] |= bit;
            this.black |= bit;
        }

    }

    public Optional<Piece> getPiece( final Vector2I pos ) {

        long bit = posToBit( pos );

        for ( PieceType type : PieceType.values() ) {
            if ( ( whitePieces[type.ordinal()] & bit ) != 0 ) {
                return Optional.of( new Piece( type, TeamColor.WHITE ) );
            } else if ( ( blackPieces[type.ordinal()] & bit ) != 0 ) {
                return Optional.of( new Piece( type, TeamColor.BLACK ) );
            }
        }

        return Optional.empty();
    }

    public Collection<Vector2I> getLegal( final Vector2I pos ) {

        Optional<Piece> pieceOptional = this.getPiece( pos );
        if ( pieceOptional.isEmpty() ) {
            return Collections.emptyList();
        }

        final Piece piece = pieceOptional.get();
        long legal = FULL_MASK;
        legal ^= this.getTeamMask( piece.getTeam() ); // only allow empty or enemy squares
        legal &= this.getMoveMask( piece, pos ); // set mask for piece move set

        // TODO more checks

        final Set<Vector2I> legalMoves = new HashSet<>();
        for ( int y = 0; y < BOARD_SIZE; y++ ) {
            for ( int x = 0; x < BOARD_SIZE; x++ ) {

                Vector2I toPos = new Vector2I( x, y );

                // If the bit at the current position is not set, it is a legal move
                if ( ( legal & posToBit( toPos ) ) != 0 ) {
                    legalMoves.add( toPos );
                }
            }
        }

        return legalMoves;
    }

    private long[] getEnemyPieces( final TeamColor team ) {
        return TeamColor.WHITE.equals( team ) ? this.blackPieces : this.whitePieces;
    }

    private long[] getTeamPieces( final TeamColor team ) {
        return TeamColor.WHITE.equals( team ) ? this.whitePieces : this.blackPieces;
    }

    private long getEnemyMask( final TeamColor team ) {
        return TeamColor.WHITE.equals( team ) ? this.black : this.white;
    }

    private long getTeamMask( final TeamColor team ) {
        return TeamColor.WHITE.equals( team ) ? this.white : this.black;
    }

    private long getFileMask( final int x ) {
        return FILE_MASK << x;
    }

    private long getRankMask( final int y ) {
        return RANK_MASK << ( y * BOARD_SIZE );
    }

    private long getDiagonalMask( final Vector2I pos ) {
        // TODO
        return 0L;
    }

    private long flipVertical( final long bitBoard ) {
        //https://www.chessprogramming.org/Flipping_Mirroring_and_Rotating
        return bitBoard ^ 56;
    }

    private long flipHorizontal( final long bitBoard ) {
        // https://www.chessprogramming.org/Flipping_Mirroring_and_Rotating
        return bitBoard ^ 7;
    }

    private long getMoveMask( final Piece piece, final Vector2I from ) {
        if ( piece == null ) {
            return EMPTY_MASK;
        }
        return switch ( piece.getType() ) {
            case PAWN -> this.getPawnMoveMask( piece.getTeam(), from );
            case KNIGHT -> this.getKnightMoveMask( from );
            case BISHOP -> this.getBishopMoveMask( from );
            case ROOK -> this.getRookMoveMask( from );
            case QUEEN -> this.getQueenMoveMask( from );
            case KING -> this.getKingMoveMask( from );
        };
    }

    private long getPawnMoveMask( final TeamColor team, final Vector2I from ) {
        final long bit = posToBit( from );
        final long pawnRankMask = TeamColor.WHITE.equals( team )
                ? this.getRankMask( 1 )
                : this.getRankMask( 6 );
        final boolean twoStep = ( bit & pawnRankMask ) != 0;
        long move = TeamColor.WHITE.equals( team )
                ? bit << BOARD_SIZE
                : bit >> BOARD_SIZE;
        if ( !twoStep ) {
            return move;
        }
        return TeamColor.WHITE.equals( team )
                ? move ^ ( bit << ( BOARD_SIZE * 2 ) )
                : move ^ ( bit >> ( BOARD_SIZE * 2 ) );
    }

    private long getKnightMoveMask( final Vector2I from ) {
        final long bit = this.posToBit( from );
        // https://www.chessprogramming.org/Knight_Pattern
        return ( bit << 15 ) ^
                ( bit << 17 ) ^
                ( bit << 10 ) ^
                ( bit << 6 ) ^
                ( bit >> 10 ) ^
                ( bit >> 17 ) ^
                ( bit >> 15 ) ^
                ( bit >> 6 );
    }

    private long getBishopMoveMask( final Vector2I from ) {
        return EMPTY_MASK;
    }

    private long getRookMoveMask( final Vector2I from ) {
        return this.getFileMask( from.x ) ^ this.getRankMask( from.y );
    }

    private long getQueenMoveMask( final Vector2I from ) {
        return this.getRookMoveMask( from ) ^ this.getDiagonalMask( from );
    }

    private long getKingMoveMask( final Vector2I from ) {
        return EMPTY_MASK;
    }

    public void logPieces() {
        Log.info( "-- BitBoard --" );
        for ( TeamColor team : TeamColor.values() ) {
            Log.info( team.name() + ": " + BitUtils.toString( this.getTeamMask( team ) ) );
            for ( PieceType type : PieceType.values() ) {
                Log.info( team.name() + "_" + type.name() + ": " + BitUtils.toString( this.getTeamPieces( team )[type.ordinal()] ) );
            }
        }
    }

}
