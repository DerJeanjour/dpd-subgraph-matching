package backend.core.notation;

import backend.core.exception.NotationParsingException;
import backend.core.model.Piece;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import backend.game.Game;
import backend.game.GameConfig;
import math.Vector2I;
import util.StringUtil;

import java.util.HashMap;
import java.util.Map;

public class FenNotation implements ChessNotation {

    public static final Map<PieceType, String> pieceCodes = Map.of(
            PieceType.PAWN, "P",
            PieceType.KNIGHT, "N",
            PieceType.BISHOP, "B",
            PieceType.ROOK, "R",
            PieceType.QUEEN, "Q",
            PieceType.KING, "K" );

    public static final Map<TeamColor, String> teamCodes = Map.of(
            TeamColor.WHITE, "w",
            TeamColor.BLACK, "b" );

    public static final String EMPTY = "-";

    @Override
    public Game read( String notation ) {
        GameConfig config = new GameConfig( notation );
        return Game.getInstance( config );
    }

    @Override
    public String write( Game game ) {

        String notation = "";

        /** PLACEMENTS **/
        for ( int row = game.getBoardSize() - 1; row >= 0; row-- ) {
            int emptyCol = 0;
            for ( int col = 0; col < game.getBoardSize(); col++ ) {
                Vector2I p = new Vector2I( col, row );
                Piece piece = game.getPiece( p );
                if ( piece != null ) {
                    if ( emptyCol > 0 ) {
                        notation += emptyCol;
                        emptyCol = 0;
                    }
                    String code = pieceCodes.get( piece.getType() );
                    if ( piece.isTeam( TeamColor.BLACK ) ) {
                        code = code.toLowerCase();
                    }
                    notation += code;
                } else {
                    emptyCol++;
                }

            }
            if ( emptyCol > 0 ) {
                notation += emptyCol;
            }
            notation += "/";
        }
        notation = notation.substring( 0, notation.length() - 1 );

        /** TEAM ON MOVE **/
        notation += " ";
        notation += teamCodes.get( game.getOnMove() );

        /** CASTLING **/
        notation += " ";
        String castleNotation = "";
        if ( game.isWhiteCanCastleKing() ) {
            castleNotation += pieceCodes.get( PieceType.KING );
        }
        if ( game.isWhiteCanCastleQueen() ) {
            castleNotation += pieceCodes.get( PieceType.QUEEN );
        }
        if ( game.isBlackCanCastleKing() ) {
            castleNotation += pieceCodes.get( PieceType.KING ).toLowerCase();
        }
        if ( game.isBlackCanCastleQueen() ) {
            castleNotation += pieceCodes.get( PieceType.QUEEN ).toLowerCase();
        }
        if ( StringUtil.isBlank( castleNotation ) ) {
            castleNotation = EMPTY;
        }
        notation += castleNotation;

        /** AU PASSANT **/
        notation += " ";
        if ( game.getAuPassantPosition() == null ) {
            notation += EMPTY;
        } else {
            notation += AlgebraicNotation.getPosCode( game.getAuPassantPosition() );
        }

        /** MOVE COUNTS */
        notation += " " + game.getHalfMoveRuleCount();
        notation += " " + game.getMoveNumber();

        return notation;
    }

    public static GameConfig makeConfig( String notation ) {
        String[] notationParts = notation.split( " " );
        if ( notationParts.length != 4 && notationParts.length != 6 ) {
            throw new NotationParsingException( "FEN Notation with only 4 or 6 parts can be parsed..." );
        }

        String placementNotation = notationParts[0].trim();
        int boardSize = readBoardSize( placementNotation );
        Map<Vector2I, Piece> placements = readPlacement( placementNotation );

        String onMoveNotation = notationParts[1].trim();
        TeamColor onMove = onMoveNotation.equals( teamCodes.get( TeamColor.WHITE ) )
                ? TeamColor.WHITE
                : TeamColor.BLACK;

        String castlingNotation = notationParts[2].trim();
        boolean whiteCastleKing = false;
        boolean whiteCastleQueen = false;
        boolean blackCastleKing = false;
        boolean blackCastleQueen = false;
        if ( !castlingNotation.equals( EMPTY ) ) {
            for ( char c : castlingNotation.toCharArray() ) {
                if ( pieceCodes.get( PieceType.KING ).equals( String.valueOf( c ) ) ) {
                    whiteCastleKing = true;
                } else if ( pieceCodes.get( PieceType.QUEEN ).equals( String.valueOf( c ) ) ) {
                    whiteCastleQueen = true;
                } else if ( pieceCodes.get( PieceType.KING ).toLowerCase().equals( String.valueOf( c ) ) ) {
                    blackCastleKing = true;
                } else if ( pieceCodes.get( PieceType.QUEEN ).toLowerCase().equals( String.valueOf( c ) ) ) {
                    blackCastleQueen = true;
                }
            }
        }

        String auPassantNotation = notationParts[3].trim();
        Vector2I auPassantPosition = null;
        if ( !auPassantNotation.equals( EMPTY ) ) {
            int col = AlgebraicNotation.getCol( auPassantNotation.charAt( 0 ) );
            int row = AlgebraicNotation.getRow( auPassantNotation.charAt( 1 ) );
            auPassantPosition = new Vector2I( col, row );
        }

        int halfMoveRuleCount = 0;
        int moveNumber = 0;
        if ( notationParts.length == 6 ) {
            String halfMoveRuleNotation = notationParts[4].trim();
            if ( !halfMoveRuleNotation.equals( EMPTY ) ) {
                halfMoveRuleCount = Integer.parseInt( halfMoveRuleNotation );
            }
            String moveNumberNotation = notationParts[5].trim();
            if ( !moveNumberNotation.equals( EMPTY ) ) {
                moveNumber = Integer.parseInt( moveNumberNotation );
            }
        }
        return new GameConfig(
                notation,
                boardSize,
                placements,
                onMove,
                whiteCastleKing,
                whiteCastleQueen,
                blackCastleKing,
                blackCastleQueen,
                auPassantPosition,
                halfMoveRuleCount,
                moveNumber
        );
    }

    public static int readBoardSize( String placement ) {

        if ( StringUtil.isBlank( placement ) ) {
            throw new IllegalArgumentException();
        }

        String[] rows = placement.split( "/" );
        return rows.length;
    }

    public static Map<Vector2I, Piece> readPlacement( String placement ) {

        if ( StringUtil.isBlank( placement ) ) {
            throw new IllegalArgumentException();
        }

        String[] rows = placement.split( "/" );
        int rowSize = rows.length;
        Map<Vector2I, Piece> placements = new HashMap<>();

        for ( int i = rowSize - 1; i >= 0; i-- ) {

            int rowIdx = rowSize - i - 1;
            String row = rows[i];

            int colIdx = 0;
            for ( Character c : row.toCharArray() ) {
                if ( Character.isDigit( c ) ) {
                    colIdx += Integer.parseInt( c.toString() );
                }
                if ( Character.isAlphabetic( c ) ) {
                    PieceType pieceType = getByFenCode( c.toString().toUpperCase() );
                    if ( pieceType != null ) {
                        TeamColor team = Character.isUpperCase( c ) ? TeamColor.WHITE : TeamColor.BLACK;
                        Piece piece = new Piece( pieceType, team );
                        placements.put( new Vector2I( colIdx, rowIdx ), piece );
                    }
                    colIdx++;
                }
            }

        }
        return placements;
    }

    private static PieceType getByFenCode( String code ) {
        for ( Map.Entry<PieceType, String> entry : pieceCodes.entrySet() ) {
            if ( entry.getValue().equals( code ) ) {
                return entry.getKey();
            }
        }
        return null;
    }

}
