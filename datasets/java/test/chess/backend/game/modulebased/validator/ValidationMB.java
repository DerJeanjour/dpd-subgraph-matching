package backend.game.modulebased.validator;

import backend.core.model.Move;
import backend.core.model.Validation;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

public class ValidationMB extends Validation {

    @Getter
    private final Set<RuleType> rulesApplied;

    public ValidationMB( Move move ) {
        super( move );
        this.rulesApplied = new HashSet<>();
    }
}
