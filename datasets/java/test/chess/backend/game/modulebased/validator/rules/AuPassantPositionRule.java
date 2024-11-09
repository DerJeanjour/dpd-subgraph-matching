package backend.game.modulebased.validator.rules;

import backend.core.model.Piece;
import backend.core.values.ActionType;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import backend.game.modulebased.GameMB;
import backend.game.modulebased.validator.Rule;
import backend.game.modulebased.validator.RuleType;
import math.Vector2I;

import java.util.Arrays;

public class AuPassantPositionRule extends Rule {

    public AuPassantPositionRule() {
        super( RuleType.AU_PASSANT_POSITION, Arrays.asList( ActionType.TRIGGER_AU_PASSANT ) );
    }

    @Override
    public boolean validate( GameMB game, Vector2I from, Vector2I to ) {
        if ( game.isType( from, PieceType.PAWN ) ) {
            int pawnLine = game.isTeam( from, TeamColor.WHITE ) ? 1 : game.getBoardSize() - 2;
            if ( from.y != pawnLine ) {
                return false;
            }
            Piece piece = game.getPiece( from );
            Piece start = game.getConfig().getPlacements().get( from );
            if ( start == null ) {
                return false;
            }
            if ( !start.isType( piece.getType() ) ) {
                return false;
            }
            return to.sub( from ).length() > 1;
        }
        return false;
    }

    @Override
    public void applyAdditionalAfterMove( GameMB game, Vector2I from, Vector2I to ) {
        game.setAuPassantPosition( to );
    }

}
