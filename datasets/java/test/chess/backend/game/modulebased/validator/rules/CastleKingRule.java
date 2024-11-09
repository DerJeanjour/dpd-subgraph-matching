package backend.game.modulebased.validator.rules;

import backend.core.values.ActionType;
import backend.core.values.Dir;
import backend.core.values.TeamColor;
import backend.game.MoveGenerator;
import backend.game.modulebased.GameMB;
import backend.game.modulebased.validator.Rule;
import backend.game.modulebased.validator.RuleType;
import math.Vector2I;

import java.util.Arrays;

public class CastleKingRule extends Rule {

    public CastleKingRule() {
        super( RuleType.CASTLING_KING_SIDE, Arrays.asList( ActionType.CASTLE_KING, ActionType.MOVE ) );
    }

    @Override
    public boolean validate( GameMB game, Vector2I from, Vector2I to ) {
        if ( !( game.isTeam( from, TeamColor.WHITE ) && game.isWhiteCanCastleKing() ) &&
                !( game.isTeam( from, TeamColor.BLACK ) && game.isBlackCanCastleKing() ) ) {
            return false;
        }
        return MoveGenerator.generateCastleKingMoves( game, from ).contains( to );
    }

    @Override
    public void applyAdditionalAfterMove( GameMB game, Vector2I from, Vector2I to ) {
        Vector2I rookPos = game.isTeam( to, TeamColor.WHITE )
                ? new Vector2I( game.getBoardSize() - 1, 0 )
                : new Vector2I( game.getBoardSize() - 1, game.getBoardSize() - 1 );
        Vector2I target = to.add( Dir.LEFT.vector );
        game.movePiece( rookPos, target );
        if ( game.isTeam( to, TeamColor.WHITE ) ) {
            game.setWhiteCanCastleKing( false );
            game.setWhiteCanCastleQueen( false );
        } else {
            game.setBlackCanCastleKing( false );
            game.setBlackCanCastleQueen( false );
        }
    }

}
