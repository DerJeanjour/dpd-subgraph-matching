package backend.bot.evaluator;

import backend.core.values.TeamColor;
import backend.game.Game;

public interface ChessEvaluator {

    /**
     * Evaluate game advantage
     *
     * @return double value between -1 and 1 (positive is favor given team and negative for enemy)
     */
    double evaluate( Game game, TeamColor color );

}
