package backend.game.modulebased.validator.rules;

import backend.core.values.ActionType;
import backend.core.values.Dir;
import backend.core.values.TeamColor;
import backend.game.MoveGenerator;
import backend.game.modulebased.GameMB;
import backend.game.modulebased.validator.Rule;
import backend.game.modulebased.validator.RuleType;
import math.Vector2I;

import java.util.Arrays;

public class CastleQueenRule extends Rule {

    public CastleQueenRule() {
        super( RuleType.CASTLING_QUEEN_SIDE, Arrays.asList( ActionType.CASTLE_QUEEN, ActionType.MOVE ) );
    }

    @Override
    public boolean validate( GameMB game, Vector2I from, Vector2I to ) {
        if ( !( game.isTeam( from, TeamColor.WHITE ) && game.isWhiteCanCastleQueen() ) &&
                !( game.isTeam( from, TeamColor.BLACK ) && game.isBlackCanCastleQueen() ) ) {
            return false;
        }
        return MoveGenerator.generateCastleQueenMoves( game, from ).contains( to );
    }

    @Override
    public void applyAdditionalAfterMove( GameMB game, Vector2I from, Vector2I to ) {
        Vector2I rookPos = game.isTeam( to, TeamColor.WHITE )
                ? new Vector2I( 0, 0 )
                : new Vector2I( 0, game.getBoardSize() - 1 );
        Vector2I target = to.add( Dir.RIGHT.vector );
        game.movePiece( rookPos, target );
        if ( game.isTeam( to, TeamColor.WHITE ) ) {
            game.setWhiteCanCastleKing( false );
            game.setWhiteCanCastleQueen( false );
        } else {
            game.setBlackCanCastleKing( false );
            game.setBlackCanCastleQueen( false );
        }
    }
}
