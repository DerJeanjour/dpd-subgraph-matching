package backend.bot;

import backend.bot.evaluator.ChessEvaluator;
import backend.core.values.TeamColor;
import backend.game.Game;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ChessBot {

    protected final TeamColor teamColor;

    protected final ChessEvaluator evaluator;

    public abstract void makeMove( Game game );

}
