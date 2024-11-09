package backend.core.exception;

import backend.game.Game;
import math.Vector2I;
import misc.Log;

public class IllegalMoveException extends RuntimeException {

    public IllegalMoveException( Game game, Vector2I from, Vector2I to, Exception e ) {
        Log.error( "Illegal {} move while moving from {} to {}:", game.getMoveNumber(), from, to );
        e.printStackTrace();
    }

}
