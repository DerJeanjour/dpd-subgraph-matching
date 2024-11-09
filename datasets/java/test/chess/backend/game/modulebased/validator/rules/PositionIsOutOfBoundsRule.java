package backend.game.modulebased.validator.rules;

import backend.core.values.ActionType;
import backend.game.modulebased.GameMB;
import backend.game.modulebased.validator.Rule;
import backend.game.modulebased.validator.RuleType;
import math.Vector2I;
import util.MathUtil;

import java.util.Arrays;

public class PositionIsOutOfBoundsRule extends Rule {

    public PositionIsOutOfBoundsRule() {
        super( RuleType.POSITION_IS_OUT_OF_BOUNDS, Arrays.asList( ActionType.MOVE ) );
    }

    @Override
    public boolean validate( GameMB game, Vector2I from, Vector2I to ) {
        return MathUtil.isOutOfBounds( to.x, game.getBoardSize() )
                || MathUtil.isOutOfBounds( to.y, game.getBoardSize() );
    }

}
