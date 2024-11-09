package backend.game.modulebased.validator.rules;

import backend.core.model.Piece;
import backend.core.values.ActionType;
import backend.core.values.TeamColor;
import backend.game.modulebased.GameMB;
import backend.game.modulebased.validator.Rule;
import backend.game.modulebased.validator.RuleType;
import math.Vector2I;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KingWouldBeInCheck extends Rule {

    public KingWouldBeInCheck() {
        super( RuleType.KING_WOULD_BE_IN_CHECK, Arrays.asList( ActionType.CHECK ) );
    }

    @Override
    public boolean validate( GameMB game, Vector2I from, Vector2I to ) {

        Piece king = game.getTeam( game.getTeam( from ) ).getKing();
        Vector2I kingPos = game.getPosition( king );

        if ( game.isAttacked( kingPos ) ) {
            if ( from.equals( kingPos ) && game.isAttacked( to ) ) {
                return true;
            } else if ( !from.equals( kingPos ) ) {

                if ( !game.isPined( to ) ) {
                    return true;
                }

                // check if all pins would be resolved
                if ( !getUnresolvedPinIdxs( game, king.getTeam() ).contains( game.getPinIdx( to ) ) ) {
                    return true;
                }

            }
        }
        if ( game.isPined( from ) && game.getPinIdx( from ) != game.getPinIdx( to ) ) {
            return true;
        }
        return false;
    }

    private List<Integer> getUnresolvedPinIdxs( GameMB game, TeamColor team ) {

        List<Integer> unresolvedPinIdxs = new ArrayList<>();
        for ( int i = 0; i < game.getPined().size(); i++ ) {
            List<Vector2I> pin = game.getPined().get( i );
            boolean resolved = false;
            for ( Vector2I p : pin ) {
                if ( game.isTeam( p, team ) ) {
                    resolved = true;
                }
            }
            if ( !resolved ) {
                unresolvedPinIdxs.add( i );
            }
        }
        return unresolvedPinIdxs;
    }

}
