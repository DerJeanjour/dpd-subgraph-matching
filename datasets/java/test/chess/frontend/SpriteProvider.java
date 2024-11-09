package frontend;

import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.imgscalr.Scalr;
import util.ResourceLoader;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class SpriteProvider {

    private static String DEFAULT_PIECE_SPRITE_PATH = "sprites/Chess_Pieces_Sprite.png";

    @Getter
    private boolean loaded;

    private Map<PieceType, BufferedImage> whiteSprites;

    private Map<PieceType, BufferedImage> blackSprites;

    public void reload( int positionSize ) {
        this.whiteSprites = getPieceSprites( positionSize, TeamColor.WHITE );
        this.blackSprites = getPieceSprites( positionSize, TeamColor.BLACK );
        this.loaded = true;
    }

    public BufferedImage getPieceSprite( PieceType type, TeamColor team ) {

        if ( !this.isLoaded() ) {
            throw new IllegalStateException( "Sprites are not loaded yet!" );
        }

        if ( TeamColor.WHITE.equals( team ) ) {
            return this.whiteSprites.get( type );
        }
        return this.blackSprites.get( type );
    }

    private Map<PieceType, BufferedImage> getPieceSprites( int targetSize, TeamColor color ) {
        Map<PieceType, BufferedImage> sprites = new HashMap<>();

        BufferedImage allPieces = ResourceLoader.getImageFile( DEFAULT_PIECE_SPRITE_PATH );
        if ( allPieces == null ) {
            return sprites;
        }

        int spriteSize = allPieces.getWidth() / 6;
        int row = color.equals( TeamColor.WHITE ) ? 0 : 1;

        BufferedImage kingSprite = allPieces.getSubimage( 0 * spriteSize, row * spriteSize, spriteSize, spriteSize );
        BufferedImage queenSprite = allPieces.getSubimage( 1 * spriteSize, row * spriteSize, spriteSize, spriteSize );
        BufferedImage bishopSprite = allPieces.getSubimage( 2 * spriteSize, row * spriteSize, spriteSize, spriteSize );
        BufferedImage knightSprite = allPieces.getSubimage( 3 * spriteSize, row * spriteSize, spriteSize, spriteSize );
        BufferedImage rookSprite = allPieces.getSubimage( 4 * spriteSize, row * spriteSize, spriteSize, spriteSize );
        BufferedImage pawnSprite = allPieces.getSubimage( 5 * spriteSize, row * spriteSize, spriteSize, spriteSize );

        sprites.put( PieceType.KING, Scalr.resize( kingSprite, Scalr.Method.BALANCED, targetSize, targetSize ) );
        sprites.put( PieceType.QUEEN, Scalr.resize( queenSprite, Scalr.Method.BALANCED, targetSize, targetSize ) );
        sprites.put( PieceType.BISHOP, Scalr.resize( bishopSprite, Scalr.Method.BALANCED, targetSize, targetSize ) );
        sprites.put( PieceType.KNIGHT, Scalr.resize( knightSprite, Scalr.Method.BALANCED, targetSize, targetSize ) );
        sprites.put( PieceType.ROOK, Scalr.resize( rookSprite, Scalr.Method.BALANCED, targetSize, targetSize ) );
        sprites.put( PieceType.PAWN, Scalr.resize( pawnSprite, Scalr.Method.BALANCED, targetSize, targetSize ) );

        return sprites;
    }

}
