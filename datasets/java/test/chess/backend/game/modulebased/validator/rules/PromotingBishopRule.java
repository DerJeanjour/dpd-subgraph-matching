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

public class PromotingBishopRule extends Rule {


    public PromotingBishopRule() {
        super( RuleType.PROMOTING_BISHOP, Arrays.asList( ActionType.PROMOTING_BISHOP ) );
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
        pawn.setType( PieceType.BISHOP );
    }
}
