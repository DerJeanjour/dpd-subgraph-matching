package backend.core.model;

import backend.core.values.ActionType;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class Validation {

    protected final Move move;

    protected final Set<ActionType> actions;

    protected boolean legal;

    public Validation( Move move ) {
        this.move = move;
        this.actions = new HashSet<>();
        this.legal = true;
    }

    public boolean hasAction() {
        return !this.actions.isEmpty();
    }

    public boolean hasAction( ActionType type ) {
        return this.actions.contains( type );
    }

}
