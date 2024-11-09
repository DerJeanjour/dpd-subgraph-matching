package backend.game.modulebased.validator;

import backend.core.values.ActionType;
import backend.game.modulebased.GameMB;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import math.Vector2I;

import java.util.List;

@Data
@RequiredArgsConstructor
public abstract class Rule {

    private final RuleType type;

    private final List<ActionType> tags;

    public abstract boolean validate( GameMB game, Vector2I from, Vector2I to );

    public void applyAdditionalAfterMove( GameMB game, Vector2I from, Vector2I to ) {
        // overwrite for additional action
        return;
    }

}
