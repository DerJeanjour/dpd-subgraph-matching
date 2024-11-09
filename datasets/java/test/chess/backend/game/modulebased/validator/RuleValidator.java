package backend.game.modulebased.validator;

import backend.core.model.Move;
import backend.core.values.ActionType;
import backend.core.values.PieceType;
import backend.game.MoveGenerator;
import backend.game.modulebased.GameMB;
import backend.game.modulebased.validator.rules.*;
import lombok.Getter;
import math.Vector2I;

import java.util.*;
import java.util.stream.Collectors;

public class RuleValidator {

    @Getter
    private final List<Rule> rules;

    private final GameMB game;

    public RuleValidator( GameMB game, List<RuleType> ruleTypes ) {
        this.game = game;
        this.rules = new ArrayList<>();
        ruleTypes.forEach( type -> addRule( type ) );
    }

    private void addRule( RuleType type ) {
        switch ( type ) {
            case POSITION_IS_OUT_OF_BOUNDS:
                this.rules.add( new PositionIsOutOfBoundsRule() );
                break;
            case GAME_IS_FINISHED:
                this.rules.add( new GameIsFinishedRule() );
                break;
            case TEAM_IS_NOT_ON_MOVE:
                this.rules.add( new TeamIsNotOnMoveRule() );
                break;
            case ALLOWED_TO_CAPTURE:
                this.rules.add( new AllowedToCaptureRule() );
                break;
            case NOT_ALLOWED_TO_CAPTURE:
                this.rules.add( new NotAllowedToCaptureRule() );
                break;
            case PAWN_MOVE:
                this.rules.add( new PawnMoveRule() );
                break;
            case BISHOP_MOVE:
                this.rules.add( new BishopMoveRule() );
                break;
            case KNIGHT_MOVE:
                this.rules.add( new KnightMoveRule() );
                break;
            case ROOK_MOVE:
                this.rules.add( new RookMoveRule() );
                break;
            case QUEEN_MOVE:
                this.rules.add( new QueenMoveRule() );
                break;
            case KING_MOVE:
                this.rules.add( new KingMoveRule() );
                break;
            case PROMOTING_QUEEN:
                this.rules.add( new PromotingQueenRule() );
                break;
            case PROMOTING_ROOK:
                this.rules.add( new PromotingRookRule() );
                break;
            case PROMOTING_BISHOP:
                this.rules.add( new PromotingBishopRule() );
                break;
            case PROMOTING_KNIGHT:
                this.rules.add( new PromotingKnightRule() );
                break;
            case AU_PASSANT_POSITION:
                this.rules.add( new AuPassantPositionRule() );
                break;
            case AU_PASSANT_CAPTURE:
                this.rules.add( new AuPassantCaptureRule() );
                break;
            case CASTLING_QUEEN_SIDE:
                this.rules.add( new CastleQueenRule() );
                break;
            case CASTLING_KING_SIDE:
                this.rules.add( new CastleKingRule() );
                break;
            case KING_WOULD_BE_IN_CHECK:
                this.rules.add( new KingWouldBeInCheck() );
                break;
        }
    }

    public void applyAdditionalActions( Set<ActionType> actions, Vector2I from, Vector2I to ) {
        for ( Rule rule : this.rules ) {
            List<ActionType> tags = rule.getTags();
            if ( actions.containsAll( tags ) ) {
                rule.applyAdditionalAfterMove( this.game, from, to );
            }
        }
    }

    public Map<Vector2I, List<ValidationMB>> validate( Vector2I from ) {
        Map<Vector2I, List<ValidationMB>> validation = new HashMap<>();
        Set<Vector2I> positions = MoveGenerator.generateAllPossibleMoves( this.game, from );
        for ( Vector2I to : positions ) {
            validation.put( to, validate( new Move( from, to ) ) );
        }
        return validation;
    }

    public List<ValidationMB> validate( Move move ) {

        ValidationMB validatedPosition = new ValidationMB( move );
        validatedPosition.setLegal( false );
        validatedPosition.getActions().add( ActionType.MOVE );

        Vector2I from = move.getFrom();
        Vector2I to = move.getTo();

        boolean isPromoting = false;

        // check if team is on move
        if ( getRule( RuleType.TEAM_IS_NOT_ON_MOVE ).validate( this.game, from, to ) ) {
            return Arrays.asList( validatedPosition );
        }

        // check if same square
        if ( from.equals( to ) ) {
            return Arrays.asList( validatedPosition );
        }

        // check out of bounds
        if ( getRule( RuleType.POSITION_IS_OUT_OF_BOUNDS ).validate( this.game, from, to ) ) {
            return Arrays.asList( validatedPosition );
        }

        // check if piece is moved
        PieceType pieceType = this.game.getType( from );
        if ( pieceType == null ) {
            return Arrays.asList( validatedPosition );
        }

        // check pawn moves
        if ( PieceType.PAWN.equals( pieceType ) ) {
            boolean validPawnMove = getRule( RuleType.PAWN_MOVE ).validate( this.game, from, to );
            boolean auPassant = getRule( RuleType.AU_PASSANT_CAPTURE ).validate( this.game, from, to );
            if ( !validPawnMove && !auPassant ) {
                return Arrays.asList( validatedPosition );
            }
            if ( auPassant ) {
                validatedPosition.getActions().add( ActionType.CAPTURE_AU_PASSANT );
            }
            if ( validPawnMove && getRule( RuleType.PROMOTING_QUEEN ).validate( this.game, from, to ) ) {
                isPromoting = true;
            }
            if ( validPawnMove && getRule( RuleType.AU_PASSANT_POSITION ).validate( this.game, from, to ) ) {
                validatedPosition.getActions().add( ActionType.TRIGGER_AU_PASSANT );
            }
        }
        // check knight moves
        if ( PieceType.KNIGHT.equals( pieceType ) ) {
            if ( !getRule( RuleType.KNIGHT_MOVE ).validate( this.game, from, to ) ) {
                return Arrays.asList( validatedPosition );
            }
        }
        // check bishop moves
        if ( PieceType.BISHOP.equals( pieceType ) ) {
            if ( !getRule( RuleType.BISHOP_MOVE ).validate( this.game, from, to ) ) {
                return Arrays.asList( validatedPosition );
            }
        }
        // check rook moves
        if ( PieceType.ROOK.equals( pieceType ) ) {
            if ( !getRule( RuleType.ROOK_MOVE ).validate( this.game, from, to ) ) {
                return Arrays.asList( validatedPosition );
            }
        }
        // check queen moves
        if ( PieceType.QUEEN.equals( pieceType ) ) {
            if ( !getRule( RuleType.QUEEN_MOVE ).validate( this.game, from, to ) ) {
                return Arrays.asList( validatedPosition );
            }
        }
        // check king moves
        if ( PieceType.KING.equals( pieceType ) ) {
            boolean validKingMove = getRule( RuleType.KING_MOVE ).validate( this.game, from, to );
            boolean validKingCastle = getRule( RuleType.CASTLING_KING_SIDE ).validate( this.game, from, to );
            boolean validQueenCastle = getRule( RuleType.CASTLING_QUEEN_SIDE ).validate( this.game, from, to );
            if ( !validKingMove && !validKingCastle && !validQueenCastle ) {
                return Arrays.asList( validatedPosition );
            }
            if ( validKingCastle ) {
                validatedPosition.getActions().add( ActionType.CASTLE_KING );
            }
            if ( validQueenCastle ) {
                validatedPosition.getActions().add( ActionType.CASTLE_QUEEN );
            }
        }

        // check if king would be check
        if ( getRule( RuleType.KING_WOULD_BE_IN_CHECK ).validate( this.game, from, to ) ) {
            return Arrays.asList( validatedPosition );
        }

        // check capture
        if ( getRule( RuleType.NOT_ALLOWED_TO_CAPTURE ).validate( this.game, from, to ) ) {
            return Arrays.asList( validatedPosition );
        }
        if ( getRule( RuleType.ALLOWED_TO_CAPTURE ).validate( this.game, from, to ) ) {
            validatedPosition.getActions().add( ActionType.CAPTURE );
        }

        if ( isPromoting ) {
            ValidationMB promoQueen = new ValidationMB( new Move( move.getFrom(), move.getTo(), PieceType.QUEEN ) );
            promoQueen.setLegal( true );
            promoQueen.getActions().addAll( validatedPosition.getActions() );
            promoQueen.getActions().add( ActionType.PROMOTING_QUEEN );
            ValidationMB promoRook = new ValidationMB( new Move( move.getFrom(), move.getTo(), PieceType.ROOK ) );
            promoRook.setLegal( true );
            promoRook.getActions().addAll( validatedPosition.getActions() );
            promoRook.getActions().add( ActionType.PROMOTING_ROOK );
            ValidationMB promoBishop = new ValidationMB( new Move( move.getFrom(), move.getTo(), PieceType.BISHOP ) );
            promoBishop.setLegal( true );
            promoBishop.getActions().addAll( validatedPosition.getActions() );
            promoBishop.getActions().add( ActionType.PROMOTING_BISHOP );
            ValidationMB promoKnight = new ValidationMB( new Move( move.getFrom(), move.getTo(), PieceType.KNIGHT ) );
            promoKnight.setLegal( true );
            promoKnight.getActions().addAll( validatedPosition.getActions() );
            promoKnight.getActions().add( ActionType.PROMOTING_KNIGHT );
            return Arrays.asList( promoQueen, promoRook, promoBishop, promoKnight );
        }

        validatedPosition.setLegal( true );
        return Arrays.asList( validatedPosition );
    }

    /**
     * Validate after move.
     * Checking check, checkmate and stalemate.
     */
    public void postValidate( ValidationMB validation ) {
        boolean isCheck = this.game.isCheckFor( this.game.getOnMove() );
        boolean hasMoves = this.game.hasLegalMovesLeft( this.game.getOnMove() );
        if ( isCheck ) {
            validation.getActions().add( ActionType.CHECK );
            if ( !hasMoves ) {
                validation.getActions().add( ActionType.CHECKMATE );
            }
        } else if ( !hasMoves ) {
            validation.getActions().add( ActionType.STALEMATE );
        }
    }

    public ValidationMB validateLegacy( Move move ) {

        ValidationMB validatedPosition = new ValidationMB( move );
        for ( Rule rule : this.rules ) {
            if ( rule.validate( this.game, move.getFrom(), move.getTo() ) ) {
                validatedPosition.getActions().addAll( rule.getTags() );
                validatedPosition.getRulesApplied().add( rule.getType() );
            }
        }

        evaluateLegality( validatedPosition );
        return validatedPosition;
    }

    private void evaluateLegality( ValidationMB validatedPosition ) {
        boolean legal = !validatedPosition.getActions().isEmpty();
        for ( RuleType appliedRule : validatedPosition.getRulesApplied() ) {
            if ( !appliedRule.legal ) {
                legal = false;
            }
        }
        if ( legal && !validationHasAnyTag( validatedPosition, ActionType.MOVE ) ) {
            legal = false;
        }
        validatedPosition.setLegal( legal );
    }

    private boolean validationHasAnyTag( ValidationMB validatedPosition, ActionType... actions ) {
        if ( actions.length == 0 ) {
            return false;
        }
        return validatedPosition.getActions().stream()
                .filter( t -> Arrays.asList( actions ).contains( t ) )
                .findAny().isPresent();
    }

    private boolean ruleHasAnyTag( Rule rule, Set<ActionType> actions ) {
        return rule.getTags().stream()
                .filter( t -> actions.contains( t ) )
                .findAny().isPresent();
    }

    public Rule getRule( RuleType type ) {
        return this.rules.stream()
                .filter( rule -> rule.getType().equals( type ) )
                .findFirst().orElse( null );
    }

    public RuleValidator clone( GameMB game ) {
        RuleValidator ruleValidator = new RuleValidator(
                game,
                getRules().stream()
                        .map( Rule::getType )
                        .collect( Collectors.toList() )
        );
        return ruleValidator;
    }

}
