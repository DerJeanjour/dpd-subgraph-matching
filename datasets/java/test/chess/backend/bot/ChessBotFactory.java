package backend.bot;

import backend.bot.alphabeta.AlphaBetaChessBot;
import backend.bot.random.RandomChessBot;
import backend.core.values.PlayerType;
import backend.core.values.TeamColor;

public class ChessBotFactory {

    public static ChessBot get( PlayerType player, TeamColor team ) {
        switch ( player ) {
            case RANDOM_BOT:
                return new RandomChessBot( team );
            case ALPHA_BETA_BOT:
                return new AlphaBetaChessBot( team );
        }
        return null;
    }

}
