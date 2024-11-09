package backend.game;

import backend.bot.ChessBot;
import backend.bot.ChessBotFactory;
import backend.core.values.PlayerType;
import backend.core.values.TeamColor;
import lombok.Getter;
import misc.Log;
import misc.Timer;

public class Player {

    private final static long WAITING_FOR_MOVE = 1000L;

    @Getter
    private final TeamColor team;

    @Getter
    private final PlayerType type;

    private final ChessBot bot;

    public Player( TeamColor team, PlayerType type ) {
        super();
        this.team = team;
        this.type = type;
        this.bot = ChessBotFactory.get( type, team );
    }

    public boolean isOnMove( Game game ) {
        return game.isOnMove( this.team );
    }

    public void makeMove( Game game ) {
        if ( !isOnMove( game ) || PlayerType.HUMAN.equals( this.type ) ) {
            return;
        }
        Log.info( "{} making move for {}...", this.type, this.team );
        try {
            Thread.sleep( WAITING_FOR_MOVE );
        } catch ( InterruptedException e ) {
            Log.error( "Something went wrong while thinking.." );
        }
        Timer timer = new Timer();
        this.bot.makeMove( game );
        Log.info( "{} needed {}m for calculation move", this.type, timer.getTimeSinceMillis() );
    }

    public boolean isHuman() {
        return PlayerType.HUMAN.equals( this.type );
    }

    public boolean isBot() {
        return !isHuman();
    }

}
