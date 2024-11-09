package backend.game;

import backend.core.model.Piece;
import backend.core.notation.FenNotation;
import backend.core.values.TeamColor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import math.Vector2I;
import util.ResourceLoader;

import java.util.List;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class GameConfig {

    private static String DEFAULT_PIECE_PLACEMENT_PATH = "placements/default_piece_placements.txt";

    private final String definition;

    private final int boardSize;

    private final Map<Vector2I, Piece> placements;

    private final TeamColor onMove;

    private final boolean whiteCanCastleKing;

    private final boolean whiteCanCastleQueen;

    private final boolean blackCanCastleKing;

    private final boolean blackCanCastleQueen;

    private final Vector2I auPassantPosition;

    private final int halfMoveRuleCount;

    private final int moveNumber;

    public GameConfig() {
        this.definition = fetchDefault();
        GameConfig config = FenNotation.makeConfig( this.definition );
        this.boardSize = config.getBoardSize();
        this.placements = config.getPlacements();
        this.onMove = config.getOnMove();
        this.whiteCanCastleKing = config.isWhiteCanCastleKing();
        this.whiteCanCastleQueen = config.isWhiteCanCastleQueen();
        this.blackCanCastleKing = config.isBlackCanCastleKing();
        this.blackCanCastleQueen = config.isBlackCanCastleQueen();
        this.auPassantPosition = config.getAuPassantPosition();
        this.halfMoveRuleCount = config.getHalfMoveRuleCount();
        this.moveNumber = config.getMoveNumber();
    }

    public GameConfig( String notation ) {
        this.definition = notation;
        GameConfig config = FenNotation.makeConfig( notation );
        this.boardSize = config.getBoardSize();
        this.placements = config.getPlacements();
        this.onMove = config.getOnMove();
        this.whiteCanCastleKing = config.isWhiteCanCastleKing();
        this.whiteCanCastleQueen = config.isWhiteCanCastleQueen();
        this.blackCanCastleKing = config.isBlackCanCastleKing();
        this.blackCanCastleQueen = config.isBlackCanCastleQueen();
        this.auPassantPosition = config.getAuPassantPosition();
        this.halfMoveRuleCount = config.getHalfMoveRuleCount();
        this.moveNumber = config.getMoveNumber();
    }

    private String fetchDefault() {

        List<String> placementLine = ResourceLoader.getTextFile( DEFAULT_PIECE_PLACEMENT_PATH );
        if ( placementLine.isEmpty() ) {
            throw new IllegalArgumentException();
        }
        // "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
        return placementLine.get( 0 );
    }

}
