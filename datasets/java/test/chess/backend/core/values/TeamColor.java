package backend.core.values;

public enum TeamColor {

    BLACK,
    WHITE;

    public static TeamColor getEnemy( TeamColor color ) {
        switch ( color ) {
            case WHITE:
                return BLACK;
            case BLACK:
                return WHITE;
        }
        return null;
    }

}
