package backend.core.notation;

import backend.game.Game;

public interface ChessNotation {

    Game read( String notation );

    String write( Game game );

}
