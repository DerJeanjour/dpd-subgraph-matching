package backend.game.modulebased.validator.rules;

import backend.core.values.ActionType;
import backend.game.modulebased.GameMB;
import backend.game.modulebased.validator.Rule;
import backend.game.modulebased.validator.RuleType;
import math.Vector2I;

import java.util.Arrays;

public class GameIsFinishedRule extends Rule {

    public GameIsFinishedRule() {
        super( RuleType.GAME_IS_FINISHED, Arrays.asList( ActionType.MOVE ) );
    }

    @Override
    public boolean validate( GameMB game, Vector2I from, Vector2I to ) {
        return game.isFinished();
    }
}
