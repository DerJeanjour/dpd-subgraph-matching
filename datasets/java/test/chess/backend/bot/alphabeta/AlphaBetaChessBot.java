package backend.bot.alphabeta;

import backend.bot.ChessBot;
import backend.bot.evaluator.PiecePointChessEvaluator;
import backend.core.model.Move;
import backend.core.model.Validation;
import backend.core.notation.ChessNotation;
import backend.core.notation.FenNotation;
import backend.core.values.ActionType;
import backend.core.values.TeamColor;
import backend.game.Game;
import misc.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AlphaBetaChessBot extends ChessBot {

    private static Map<TeamColor, Boolean> roles = Map.of(
            TeamColor.WHITE, true,
            TeamColor.BLACK, false
    );

    private static final int MAX_DEPTH = 4;

    private long searchedNodes;

    private long totalNodes;

    public AlphaBetaChessBot( TeamColor teamColor ) {
        super( teamColor, new PiecePointChessEvaluator() );
    }

    @Override
    public void makeMove( Game game ) {
        this.searchedNodes = 0l;
        this.totalNodes = 0l;
        if ( !game.isFinished() && game.isOnMove( this.teamColor ) ) {

            ChessNotation fen = new FenNotation();
            Game sandbox = fen.read( fen.write( game ) );

            this.alphaBeta( sandbox, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, roles.get( sandbox.getOnMove() ) );
            game.makeMove( sandbox.getLastMove().getMove() );
        }
    }

    private double alphaBeta( Game game, int depth, double alpha, double beta, boolean isMaximizing ) {

        if ( game.isFinished() || depth == MAX_DEPTH ) {
            return this.evaluator.evaluate( game, TeamColor.WHITE );
        }

        List<Validation> validations = game.getPossibleValidations( game.getOnMove() );
        validations = this.sortValidations( validations );
        List<Move> bestMoves = new ArrayList<>();

        double bestEval = isMaximizing
                ? Double.NEGATIVE_INFINITY
                : Double.POSITIVE_INFINITY;

        this.totalNodes += validations.size();
        for ( Validation validation : validations ) {

            this.searchedNodes++;

            final Move move = validation.getMove();
            game.makeMove( move );
            double eval = this.alphaBeta( game, depth + 1, alpha, beta, roles.get( game.getOnMove() ) );
            game.undoLastMove();

            if ( isMaximizing ) {
                if ( eval > bestEval ) {
                    bestMoves.clear();
                }
                if ( eval >= bestEval ) {
                    bestMoves.add( move );
                    bestEval = eval;
                }
                alpha = Math.max( alpha, bestEval );
            } else {
                if ( eval < bestEval ) {
                    bestMoves.clear();
                }
                if ( eval <= bestEval ) {
                    bestMoves.add( move );
                    bestEval = eval;
                }
                beta = Math.min( beta, bestEval );
            }

            // prune
            if ( beta <= alpha ) {
                break;
            }

        }

        if ( depth == 0 ) {
            Move bestMove = null;
            if ( !bestMoves.isEmpty() ) {
                bestMove = bestMoves.get( new Random().nextInt( bestMoves.size() ) );
            }
            Log.info( "Best move found for depth {}: {} ({}) - nodes {}/{} searched",
                    MAX_DEPTH, bestMove, bestEval, this.searchedNodes, this.totalNodes );
            game.makeMove( bestMove );
        }

        return bestEval;

    }

    private List<Validation> sortValidations( List<Validation> validations ) {

        List<Validation> sorted = new ArrayList<>();
        List<Validation> highPrio = new ArrayList<>();
        List<Validation> midPrio = new ArrayList<>();
        List<Validation> lowPrio = new ArrayList<>();

        for ( Validation validation : validations ) {
            if ( validation.getActions().contains( ActionType.CASTLE_KING ) ||
                    validation.getActions().contains( ActionType.CASTLE_QUEEN ) ||
                    validation.getActions().contains( ActionType.PROMOTING_QUEEN ) ||
                    validation.getActions().contains( ActionType.PROMOTING_ROOK ) ||
                    validation.getActions().contains( ActionType.PROMOTING_BISHOP ) ||
                    validation.getActions().contains( ActionType.PROMOTING_KNIGHT ) ) {
                highPrio.add( validation );
            } else if ( validation.getActions().contains( ActionType.CAPTURE ) ||
                    validation.getActions().contains( ActionType.CAPTURE_AU_PASSANT ) ) {
                midPrio.add( validation );
            } else {
                lowPrio.add( validation );
            }
        }

        sorted.addAll( highPrio );
        sorted.addAll( midPrio );
        sorted.addAll( lowPrio );
        return sorted;
    }

}
