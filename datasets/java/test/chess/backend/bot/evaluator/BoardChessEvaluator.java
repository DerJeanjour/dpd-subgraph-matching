package backend.bot.evaluator;

import backend.core.model.MoveHistory;
import backend.core.values.ActionType;
import backend.core.values.GameState;
import backend.core.values.TeamColor;
import backend.game.Game;

import java.util.Map;

public class BoardChessEvaluator implements ChessEvaluator {

    private final static Map<ActionType, Integer> actionTypePoints = Map.ofEntries(
            Map.entry( ActionType.CHECK, 10 ),
            Map.entry( ActionType.CHECKMATE, Integer.MAX_VALUE ),
            Map.entry( ActionType.STALEMATE, -10 ),
            Map.entry( ActionType.CAPTURE, 5 ),
            Map.entry( ActionType.CAPTURE_AU_PASSANT, 5 ),
            Map.entry( ActionType.PROMOTING_QUEEN, 10 ),
            Map.entry( ActionType.PROMOTING_ROOK, 8 ),
            Map.entry( ActionType.PROMOTING_BISHOP, 5 ),
            Map.entry( ActionType.PROMOTING_KNIGHT, 5 ),
            Map.entry( ActionType.CASTLE_KING, 8 ),
            Map.entry( ActionType.CASTLE_QUEEN, 8 )
    );

    @Override
    public double evaluate( Game game, TeamColor teamColor ) {
        int teamPoints = PiecePointChessEvaluator.getTeamPoints( game.getTeam( teamColor ) );
        teamPoints += getActionPoints( game, teamColor );
        int enemyPoints = PiecePointChessEvaluator.getTeamPoints( game.getTeam( game.getEnemy( teamColor ) ) );
        enemyPoints += getActionPoints( game, game.getEnemy( teamColor ) );
        return ( ( double ) teamPoints / ( double ) ( teamPoints + enemyPoints ) ) * 2d - 1d;
    }

    // FIXME this is shit
    public static int getActionPoints( Game game, TeamColor teamColor ) {

        if ( game.isFinished() ) {
            if ( game.getState().equals( GameState.TIE ) ) {
                return -actionTypePoints.get( ActionType.CHECKMATE );
            }
            if ( teamColor.equals( TeamColor.WHITE ) ) {
                if ( game.getState().equals( GameState.WHITE_WON ) ) {
                    return actionTypePoints.get( ActionType.CHECKMATE );
                }
                if ( game.getState().equals( GameState.BLACK_WON ) ) {
                    return -actionTypePoints.get( ActionType.CHECKMATE );
                }
            }
        }

        int actionPoints = 0;
        if ( game.isCheckFor( game.getEnemy( teamColor ) ) ) {
            actionPoints += actionTypePoints.get( ActionType.CHECK );
        }

        MoveHistory lastMove = game.getLastMove();
        if ( lastMove != null ) {
            for ( ActionType action : actionTypePoints.keySet() ) {
                if ( lastMove.getActions().contains( action ) ) {
                    int actionTypePoint = actionTypePoints.get( action );
                    actionPoints += lastMove.getTeam().equals( teamColor ) ? actionTypePoint : -actionPoints;
                }
            }
        }

        return actionPoints;
    }

}
