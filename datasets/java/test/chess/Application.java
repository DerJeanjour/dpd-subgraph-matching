import backend.game.Game;
import backend.game.GameConfig;
import frontend.GameView;

public class Application {

    public static void main( String[] args ) {
        GameConfig config = new GameConfig();
        Game game = Game.getInstance( config, true );
        new GameView( game, 500, 600, 600 );
    }

}
