package backend.game.modulebased.validator.rules;

import backend.core.model.Piece;
import backend.core.values.ActionType;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import backend.game.modulebased.GameMB;
import backend.game.modulebased.validator.Rule;
import backend.game.modulebased.validator.RuleType;
import math.Vector2I;

import java.util.Arrays;

public class PromotingKnightRule extends Rule {

    public PromotingKnightRule() {
        super( RuleType.PROMOTING_KNIGHT, Arrays.asList( ActionType.PROMOTING_KNIGHT ) );
    }

    @Override
    public boolean validate( GameMB game, Vector2I from, Vector2I to ) {

        if ( !game.isType( from, PieceType.PAWN ) ) {
            return false;
        }

        int enemyRank = game.isTeam( from, TeamColor.WHITE ) ? game.getBoardSize() - 1 : 0;
        return to.y == enemyRank;
    }

    @Override
    public void applyAdditionalAfterMove( GameMB game, Vector2I from, Vector2I to ) {
        Piece pawn = game.getPiece( to );
        pawn.setType( PieceType.KNIGHT );
    }
}
