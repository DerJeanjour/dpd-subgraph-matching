package backend.game.modulebased.validator.rules;

import backend.core.values.ActionType;
import backend.game.MoveGenerator;
import backend.game.modulebased.GameMB;
import backend.game.modulebased.validator.Rule;
import backend.game.modulebased.validator.RuleType;
import math.Vector2I;

import java.util.Arrays;
import java.util.Set;

public class PawnMoveRule extends Rule {

    public PawnMoveRule() {
        super( RuleType.PAWN_MOVE, Arrays.asList( ActionType.MOVE ) );
    }

    @Override
    public boolean validate( GameMB game, Vector2I from, Vector2I to ) {
        Set<Vector2I> moves = MoveGenerator.generatePawnMoves( game, from );
        Set<Vector2I> attacking = MoveGenerator.generatePawnNormalAttackingMoves( game, from );
        for ( Vector2I attack : attacking ) {
            if ( !game.hasPiece( to ) ) {
                moves.remove( attack );
            }
        }

        return moves.contains( to );
    }

}
