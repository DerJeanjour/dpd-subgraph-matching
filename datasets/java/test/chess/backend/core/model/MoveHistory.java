package backend.core.model;

import backend.core.values.ActionType;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Data
@RequiredArgsConstructor
public class MoveHistory {

    private final int number;

    private final Set<ActionType> actions;

    private final TeamColor team;

    private final PieceType piece;

    private final Move move;

}
