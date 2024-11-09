package backend.game.modulebased.validator;

public enum RuleType {

    GAME_IS_FINISHED( false ),
    POSITION_IS_OUT_OF_BOUNDS( false ),

    TEAM_IS_NOT_ON_MOVE( false ),
    ALLOWED_TO_CAPTURE( true ),
    NOT_ALLOWED_TO_CAPTURE( false ),

    PAWN_MOVE( true ),
    BISHOP_MOVE( true ),
    KNIGHT_MOVE( true ),
    ROOK_MOVE( true ),
    QUEEN_MOVE( true ),
    KING_MOVE( true ),

    PROMOTING_QUEEN( true ),
    PROMOTING_ROOK( true ),
    PROMOTING_BISHOP( true ),
    PROMOTING_KNIGHT( true ),
    AU_PASSANT_POSITION( true ),
    AU_PASSANT_CAPTURE( true ),
    CASTLING_QUEEN_SIDE( true ),
    CASTLING_KING_SIDE( true ),

    KING_WOULD_BE_IN_CHECK( false );

    public final boolean legal;

    RuleType( boolean legal ) {
        this.legal = legal;
    }

}
