package backend.bot.random;

import backend.bot.ChessBot;
import backend.core.model.Move;
import backend.core.model.Piece;
import backend.core.model.Team;
import backend.core.values.TeamColor;
import backend.game.Game;

import java.util.List;
import java.util.Random;

public class RandomChessBot extends ChessBot {

    private final Random random;

    public RandomChessBot( TeamColor teamColor ) {
        super( teamColor, null );
        this.random = new Random();
    }

    @Override
    public void makeMove( Game game ) {
        if ( !game.isFinished() && game.isOnMove( this.teamColor ) ) {

            Team team = game.getTeam( this.teamColor );
            List<Piece> alive = team.getAlive();

            while ( !alive.isEmpty() ) {

                int randomPieceIdx = this.random.nextInt( alive.size() );
                Piece randomPiece = alive.get( randomPieceIdx );
                List<Move> legalMoves = game.getPossibleMoves( randomPiece );
                if ( !legalMoves.isEmpty() ) {
                    int randomMoveIdx = this.random.nextInt( legalMoves.size() );
                    Move legalMove = legalMoves.get( randomMoveIdx );
                    game.makeMove( legalMove );
                    return;
                }

                alive.remove( randomPieceIdx );
            }

            throw new IllegalStateException( "Cant make move..." );

        }
    }

}
