package backend.bot.evaluator;

import backend.core.model.Piece;
import backend.core.model.Team;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import backend.game.Game;

import java.util.List;
import java.util.Map;

public class PiecePointChessEvaluator implements ChessEvaluator {

    private static Map<PieceType, Integer> pieceValues = Map.of(
            PieceType.PAWN, 1,
            PieceType.KNIGHT, 3,
            PieceType.BISHOP, 3,
            PieceType.ROOK, 5,
            PieceType.QUEEN, 8,
            PieceType.KING, Integer.MAX_VALUE
    );

    @Override
    public double evaluate( Game game, TeamColor teamColor ) {
        int teamPoints = getTeamPoints( game.getTeam( teamColor ) );
        int enemyPoints = getTeamPoints( game.getTeam( game.getEnemy( teamColor ) ) );
        return ( ( double ) teamPoints / ( double ) ( teamPoints + enemyPoints ) ) * 2d - 1d;
    }

    public static int getTeamPoints( Team team ) {
        if ( !team.getKing().isAlive() ) {
            return 0;
        }
        List<Piece> alivePieces = team.getAlive();
        int sum = 0;
        for ( Piece piece : alivePieces ) {
            if ( !piece.isType( PieceType.KING ) ) {
                sum += pieceValues.get( piece.getType() );
            }
        }
        return sum;
    }

}
