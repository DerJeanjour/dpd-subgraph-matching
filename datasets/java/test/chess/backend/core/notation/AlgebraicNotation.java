package backend.core.notation;

import backend.core.exception.NotationParsingException;
import backend.core.model.Move;
import backend.core.model.MoveHistory;
import backend.core.model.Piece;
import backend.core.model.Team;
import backend.core.values.ActionType;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import backend.game.Game;
import backend.game.GameConfig;
import math.Vector2I;
import util.CollectionUtil;
import util.StringUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validate with: https://www.dcode.fr/san-chess-notation
 */
public class AlgebraicNotation implements ChessNotation {

    public static final Map<PieceType, String> pieceCodes = Map.of(
            PieceType.PAWN, "",
            PieceType.KNIGHT, "N",
            PieceType.BISHOP, "B",
            PieceType.ROOK, "R",
            PieceType.QUEEN, "Q",
            PieceType.KING, "K" );

    public static final Map<ActionType, String> actionCodes = Map.ofEntries(
            Map.entry( ActionType.MOVE, "" ),
            Map.entry( ActionType.CAPTURE, "x" ),
            Map.entry( ActionType.CAPTURE_AU_PASSANT, "x" ),
            Map.entry( ActionType.PROMOTING_QUEEN, "=Q" ),
            Map.entry( ActionType.PROMOTING_ROOK, "=R" ),
            Map.entry( ActionType.PROMOTING_BISHOP, "=B" ),
            Map.entry( ActionType.PROMOTING_KNIGHT, "=N" ),
            Map.entry( ActionType.CASTLE_KING, "O-O" ),
            Map.entry( ActionType.CASTLE_QUEEN, "O-O-O" ),
            Map.entry( ActionType.CHECK, "+" ),
            Map.entry( ActionType.CHECKMATE, "#" ),
            Map.entry( ActionType.STALEMATE, "1/2-1/2" ) );

    @Override
    public Game read( String notation ) {
        GameConfig config = new GameConfig();
        Game game = Game.getInstance( config );
        applyMoves( game, notation );
        return game;
    }

    public static void applyMoves( Game game, String notation ) {
        List<String> moveNotations = Arrays.asList( notation.split( " " ) );
        moveNotations = moveNotations.stream().filter( m -> !m.endsWith( "." ) ).collect( Collectors.toList() );
        for ( String moveNotation : moveNotations ) {
            Move move = readMove( game, moveNotation );
            game.makeMove( move );
        }
    }

    public static Move readMove( Game game, String moveNotation ) {

        if ( StringUtil.isBlank( moveNotation ) ) {
            throw new NotationParsingException( "Notation is empty!" );
        }

        // cleanup
        moveNotation = moveNotation.trim();
        if ( moveNotation.contains( "." ) ) {
            moveNotation = moveNotation.substring( moveNotation.indexOf( '.' ) + 1, moveNotation.length() );
        }

        PieceType promotingMode = PieceType.QUEEN;
        if ( moveNotation.contains( "=" ) ) {
            if ( moveNotation.contains( actionCodes.get( ActionType.PROMOTING_QUEEN ) ) ) {
                promotingMode = PieceType.QUEEN;
                moveNotation = moveNotation.replace( actionCodes.get( ActionType.PROMOTING_QUEEN ), "" );
            } else if ( moveNotation.contains( actionCodes.get( ActionType.PROMOTING_ROOK ) ) ) {
                promotingMode = PieceType.ROOK;
                moveNotation = moveNotation.replace( actionCodes.get( ActionType.PROMOTING_ROOK ), "" );
            } else if ( moveNotation.contains( actionCodes.get( ActionType.PROMOTING_BISHOP ) ) ) {
                promotingMode = PieceType.BISHOP;
                moveNotation = moveNotation.replace( actionCodes.get( ActionType.PROMOTING_BISHOP ), "" );
            } else if ( moveNotation.contains( actionCodes.get( ActionType.PROMOTING_KNIGHT ) ) ) {
                promotingMode = PieceType.KNIGHT;
                moveNotation = moveNotation.replace( actionCodes.get( ActionType.PROMOTING_KNIGHT ), "" );
            }
        }


        // special cases
        if ( moveNotation.equals( actionCodes.get( ActionType.CASTLE_KING ) ) ) {
            Team onMove = game.getTeam( game.getOnMove() );
            Vector2I kingPos = game.getPosition( onMove.getKing() );
            return new Move( kingPos, kingPos.add( new Vector2I( 2, 0 ) ) );
        }

        if ( moveNotation.equals( actionCodes.get( ActionType.CASTLE_QUEEN ) ) ) {
            Team onMove = game.getTeam( game.getOnMove() );
            Vector2I kingPos = game.getPosition( onMove.getKing() );
            return new Move( kingPos, kingPos.add( new Vector2I( -2, 0 ) ) );
        }

        if ( moveNotation.equals( actionCodes.get( ActionType.STALEMATE ) ) ) {
            return new Move( null, null );
        }

        // general parsing
        char[] moveElements = moveNotation.toCharArray();

        // mandatory piece notation (PAWN is default)
        PieceType pieceType = PieceType.PAWN;

        // for ambiguous positions
        Integer optionalFromCol = null;
        Integer optionalFromRow = null;

        // mandatory position vector
        Integer toCol = null;
        Integer toRow = null;

        for ( final char m : moveElements ) {
            if ( Character.isUpperCase( m ) ) {
                PieceType pieceTypeTemp = CollectionUtil.getValueKeys( pieceCodes, String.valueOf( m ) )
                        .findFirst().orElse( null );
                if ( pieceTypeTemp != null ) {
                    pieceType = pieceTypeTemp;
                }
            }
            if ( Character.isLowerCase( m ) ) {
                if ( toCol != null ) {
                    optionalFromCol = toCol;
                }
                toCol = getCol( m );
            }
            if ( Character.isDigit( m ) ) {
                if ( toRow != null ) {
                    optionalFromRow = toRow;
                }
                toRow = getRow( m );
            }
        }

        if ( pieceType == null || toCol == null || toRow == null ) {
            throw new NotationParsingException( "No piece or target defined... ({})", moveNotation );
        }

        Vector2I to = new Vector2I( toCol, toRow );

        Team onMove = game.getTeam( game.getOnMove() );
        List<Piece> pieces = onMove.getPiecesByType( pieceType, true );
        Piece piece = null;
        if ( pieces.size() == 1 ) {
            piece = pieces.get( 0 );
        } else {

            // get ambiguous positions with legal moves
            List<Vector2I> ambiguousPositions = new ArrayList<>();
            for ( Piece p : pieces ) {
                Vector2I pPos = game.getPosition( p );
                if ( pPos != null && game.isLegal( new Move( pPos, to ) ) ) {
                    ambiguousPositions.add( pPos );
                    piece = p;
                }
            }


            if ( ambiguousPositions.size() == 1 ) {
                piece = game.getPiece( ambiguousPositions.get( 0 ) );
            } else {
                // get correct move of ambiguous positions
                if ( optionalFromCol != null && optionalFromRow != null ) {
                    piece = game.getPiece( new Vector2I( optionalFromCol, optionalFromRow ) );
                } else {
                    if ( optionalFromCol != null ) {
                        final int col = optionalFromCol;
                        Vector2I target = ambiguousPositions.stream()
                                .filter( v -> v.x == col )
                                .findFirst().orElse( null );
                        piece = game.getPiece( target );
                    }
                    if ( optionalFromRow != null ) {
                        final int row = optionalFromRow;
                        Vector2I target = ambiguousPositions.stream()
                                .filter( v -> v.y == row )
                                .findFirst().orElse( null );
                        piece = game.getPiece( target );
                    }
                }
            }

        }
        if ( piece == null ) {
            throw new NotationParsingException( "No piece found for move... ({})", moveNotation );
        }
        Vector2I fromPos = game.getPosition( piece );
        if ( fromPos == null ) {
            throw new NotationParsingException( "No position found for piece {}... ({})", piece, moveNotation );
        }

        return new Move( fromPos, to, promotingMode );
    }

    public static int getRow( char rowCode ) {
        return Character.getNumericValue( rowCode ) - 1;
    }

    public static int getCol( char colCode ) {
        return colCode - 'a';
    }

    @Override
    public String write( Game game ) {
        String notation = "";
        Game sandbox = Game.getInstance( game.getConfig() );
        for ( MoveHistory history : game.getHistory() ) {
            notation += writeCode( sandbox, history );
            sandbox.makeMove( history.getMove() );
        }
        return notation;
    }

    public static String writeCode( Game game, MoveHistory moveHistory ) {

        String pattern = "{0}{1}{2}{3}{4}{5} ";

        boolean isMoveEnd = TeamColor.BLACK.equals( moveHistory.getTeam() );
        String moveNumber = isMoveEnd ? "" : ( moveHistory.getNumber() + 1 ) + ". ";
        String pieceCode = pieceCodes.get( moveHistory.getPiece() );
        String actionCode = "";
        String posCode = getPosCode( moveHistory.getMove().getTo() );
        String actionCodePromoting = "";
        String actionCodeCheck = "";

        for ( ActionType actionType : moveHistory.getActions() ) {
            switch ( actionType ) {
                case MOVE:
                    if ( StringUtil.isBlank( actionCode ) ) {
                        actionCode = actionCodes.get( actionType );
                    }
                    break;
                case CAPTURE:
                case CAPTURE_AU_PASSANT:
                    actionCode = actionCodes.get( actionType );
                    break;
                case PROMOTING_QUEEN:
                case PROMOTING_ROOK:
                case PROMOTING_BISHOP:
                case PROMOTING_KNIGHT:
                    actionCodePromoting = actionCodes.get( actionType );
                    break;
                case CASTLE_QUEEN:
                case CASTLE_KING:
                    pieceCode = "";
                    posCode = "";
                    actionCode = actionCodes.get( actionType );
                    break;
                case CHECK:
                    if ( !moveHistory.getActions().contains( ActionType.CHECKMATE ) ) {
                        actionCodeCheck = actionCodes.get( actionType );
                    }
                    break;
                case CHECKMATE:
                    actionCodeCheck = actionCodes.get( actionType );
                    break;
                case STALEMATE:
                    actionCodeCheck = " " + actionCodes.get( actionType );
                    break;
            }
        }

        // special notation for pawns
        if ( PieceType.PAWN.equals( moveHistory.getPiece() )
                && ( moveHistory.getActions().contains( ActionType.CAPTURE ) || moveHistory.getActions().contains( ActionType.CAPTURE_AU_PASSANT ) ) ) {
            pieceCode = getColCode( moveHistory.getMove().getFrom() );
        }
        if ( !PieceType.PAWN.equals( moveHistory.getPiece() ) ) {
            pieceCode += handleAmbiguity( game, moveHistory );
        }

        return MessageFormat.format( pattern,
                moveNumber,
                pieceCode,
                actionCode,
                posCode,
                actionCodePromoting,
                actionCodeCheck
        );
    }

    /**
     * https://chess.stackexchange.com/a/1819
     * 1. check if piece move is ambiguous
     * 2. check if piece is distinguishable by column
     * 3. check if piece is distinguishable by row
     * 4. use row/column
     */
    private static String handleAmbiguity( Game game, MoveHistory moveHistory ) {

        Team onMove = game.getTeam( moveHistory.getTeam() );
        List<Piece> pieces = onMove.getPiecesByType( moveHistory.getPiece(), true );

        if ( pieces.size() == 1 ) {
            return "";
        }

        final Vector2I from = moveHistory.getMove().getFrom();

        List<Vector2I> ambiguousPositions = new ArrayList<>();
        for ( Piece piece : pieces ) {
            Vector2I piecePos = game.getPosition( piece );
            if ( !piecePos.equals( from ) && game.isLegal( moveHistory.getMove() ) ) {
                ambiguousPositions.add( piecePos );
            }
        }

        if ( ambiguousPositions.size() == 0 ) {
            return "";
        }

        // check col
        if ( !ambiguousPositions.stream().anyMatch( v -> v.x == from.x ) ) {
            return getColCode( from );
        }

        // check row
        if ( !ambiguousPositions.stream().anyMatch( v -> v.y == from.y ) ) {
            return getRowCode( from );
        }

        return getPosCode( from );
    }

    public static String getPosCode( Vector2I p ) {
        return getColCode( p ) + getRowCode( p );
    }

    public static String getRowCode( Vector2I p ) {
        return String.valueOf( p.y + 1 );
    }

    public static String getColCode( Vector2I p ) {
        return Character.toString( ( char ) ( 'a' + p.x ) );
    }

}
