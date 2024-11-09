package backend.game;

import backend.core.model.Piece;
import backend.core.values.Dir;
import backend.core.values.PieceType;
import backend.core.values.TeamColor;
import math.Vector2I;

import java.util.*;
import java.util.stream.Collectors;

public class MoveGenerator {

    public static Set<Vector2I> generateAttackedPositionsBy( Game game, TeamColor color ) {
        Set<Vector2I> attacked = new HashSet<>();
        List<Piece> alive = game.getTeam( color ).getAlive();
        for ( Piece piece : alive ) {
            Vector2I p = game.getPosition( piece );
            if ( p != null ) {
                attacked.addAll( generateAttackingMoves( game, p ) );
            }
        }
        return attacked;
    }

    public static List<List<Vector2I>> generatePinedPositionsBy( Game game, TeamColor color ) {
        List<List<Vector2I>> pinningRays = new ArrayList<>();
        Piece king = game.getTeam( game.getEnemy( color ) ).getKing();
        if ( !king.isAlive() ) {
            return pinningRays;
        }
        Vector2I kingPos = game.getPosition( king );
        for ( Piece piece : game.getTeam( color ).getAlive() ) {
            Vector2I p = game.getPosition( piece );
            switch ( piece.getType() ) {
                case BISHOP -> Dir.diagonalDirs().forEach( dir ->
                        pinningRays.add( getPinedOfRay( game, kingPos, king.getTeam(), p, dir.vector ) )
                );
                case ROOK -> Dir.baseDirs().forEach( dir ->
                        pinningRays.add( getPinedOfRay( game, kingPos, king.getTeam(), p, dir.vector ) )
                );
                case QUEEN -> Arrays.asList( Dir.values() ).forEach( dir ->
                        pinningRays.add( getPinedOfRay( game, kingPos, king.getTeam(), p, dir.vector ) )
                );
                case KNIGHT -> {
                    if ( generateKnightMoves( game, p ).contains( kingPos ) ) {
                        pinningRays.add( Arrays.asList( p ) );
                    }
                }
                case PAWN -> {
                    if ( generatePawnNormalAttackingMoves( game, p ).contains( kingPos ) ) {
                        pinningRays.add( Arrays.asList( p ) );
                    }
                }
            }
        }
        return pinningRays;
    }

    private static List<Vector2I> getPinedOfRay( Game game, Vector2I kingPos, TeamColor kingTeam, Vector2I from, Vector2I dir ) {
        List<Vector2I> positions = getPositionsOfDir( game, from, dir, -1, true, false, false, false );
        if ( positions.contains( kingPos ) ) {

            List<Vector2I> pinedRay = new ArrayList<>();
            pinedRay.add( from );
            int foundPined = 0;
            boolean foundKing = false;

            Iterator<Vector2I> iter = positions.iterator();
            while ( !foundKing && iter.hasNext() ) {
                Vector2I p = iter.next();
                if ( p.equals( kingPos ) ) {
                    foundKing = true;
                } else {
                    pinedRay.add( p );
                    if ( game.hasPiece( p ) ) {
                        foundPined++;
                    }
                }
            }
            if ( foundPined <= 1 ) {
                return pinedRay;
            }
        }
        return Collections.emptyList();
    }

    public static List<List<Vector2I>> generatePositionsOfRaysFor( Game game, TeamColor color ) {
        List<List<Vector2I>> rays = new ArrayList<>();
        for ( Piece piece : game.getTeam( color ).getAlive() ) {
            Vector2I p = game.getPosition( piece );
            switch ( piece.getType() ) {
                case BISHOP -> Dir.diagonalDirs().forEach( dir ->
                        rays.add( getPositionsOfDir( game, p, dir.vector, -1, true, false, false, false ) )
                );
                case ROOK -> Dir.baseDirs().forEach( dir ->
                        rays.add( getPositionsOfDir( game, p, dir.vector, -1, true, false, false, false ) )
                );
                case QUEEN -> Arrays.asList( Dir.values() ).forEach( dir ->
                        rays.add( getPositionsOfDir( game, p, dir.vector, -1, true, false, false, false ) )
                );
            }
        }
        return rays;
    }

    public static Set<Vector2I> generateAttackingMoves( Game game, Vector2I from ) {
        Set<Vector2I> allowed = new HashSet<>();
        allowed.addAll( generatePawnAttackingMoves( game, from ) );
        allowed.addAll( generateKnightMoves( game, from ) );
        allowed.addAll( generateBishopAttackingMoves( game, from ) );
        allowed.addAll( generateRookAttackingMoves( game, from ) );
        allowed.addAll( generateQueenAttackingMoves( game, from ) );
        allowed.addAll( generateKingMoves( game, from ) );
        return allowed;
    }

    public static Set<Vector2I> generateAllPossibleMoves( Game game, Vector2I from ) {
        Set<Vector2I> allowed = new HashSet<>();
        allowed.addAll( generatePawnMoves( game, from ) );
        allowed.addAll( generateAuPassantMoves( game, from ) );
        allowed.addAll( generateKnightMoves( game, from ) );
        allowed.addAll( generateBishopMoves( game, from ) );
        allowed.addAll( generateRookMoves( game, from ) );
        allowed.addAll( generateQueenMoves( game, from ) );
        allowed.addAll( generateKingMoves( game, from ) );
        allowed.addAll( generateCastleKingMoves( game, from ) );
        allowed.addAll( generateCastleQueenMoves( game, from ) );
        return allowed;
    }

    public static Set<Vector2I> generatePawnMoves( Game game, Vector2I from ) {

        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.PAWN ) ) {
            return allowed;
        }

        Piece piece = game.getPiece( from );
        Vector2I dir = piece.isTeam( TeamColor.WHITE ) ? Dir.UP.vector : Dir.DOWN.vector;

        int pawnLine = piece.isTeam( TeamColor.WHITE ) ? 1 : game.getBoardSize() - 2;
        boolean pawnMoved = from.y != pawnLine;
        int distance = pawnMoved ? 1 : 2;

        allowed.addAll( getPositionsOfDir( game, from, dir, distance, false, false, false, false ) );
        allowed.addAll( generatePawnNormalAttackingMoves( game, from ) );

        return allowed;
    }

    public static Set<Vector2I> generatePawnAttackingMoves( Game game, Vector2I from ) {
        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.PAWN ) ) {
            return allowed;
        }

        allowed.addAll( generatePawnNormalAttackingMoves( game, from ) );
        allowed.addAll( generateAuPassantMoves( game, from ) );

        return allowed;
    }

    public static Set<Vector2I> generatePawnNormalAttackingMoves( Game game, Vector2I from ) {
        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.PAWN ) ) {
            return allowed;
        }

        Piece piece = game.getPiece( from );
        Vector2I dir = piece.isTeam( TeamColor.WHITE ) ? Dir.UP.vector : Dir.DOWN.vector;

        Vector2I diagonalLeft = from.add( dir ).add( Dir.LEFT.vector );
        if ( !game.isOutOfBounds( diagonalLeft ) ) {
            allowed.add( diagonalLeft );
        }
        Vector2I diagonalRight = from.add( dir ).add( Dir.RIGHT.vector );
        if ( !game.isOutOfBounds( diagonalRight ) ) {
            allowed.add( diagonalRight );
        }

        return allowed;
    }

    public static Set<Vector2I> generateAuPassantMoves( Game game, Vector2I from ) {
        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.PAWN ) ) {
            return allowed;
        }
        Vector2I dir = game.isTeam( from, TeamColor.WHITE ) ? Dir.UP.vector : Dir.DOWN.vector;
        int enemyGroundLine = game.isTeam( from, TeamColor.WHITE ) ? game.getBoardSize() - 1 : 0;
        if ( from.add( dir.mul( 3 ) ).y != enemyGroundLine ) {
            return allowed;
        }
        Vector2I[] targets = new Vector2I[]{ from.add( Dir.LEFT.vector ), from.add( Dir.RIGHT.vector ) };
        for ( Vector2I target : targets ) {
            if ( !game.isOutOfBounds( target ) && game.isType( target, PieceType.PAWN ) ) {
                allowed.add( target.add( dir ) );
            }
        }

        return allowed;
    }

    public static Set<Vector2I> generateKnightMoves( Game game, Vector2I from ) {

        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.KNIGHT ) ) {
            return allowed;
        }

        allowed.add( from.add( Dir.LEFT.vector.add( Dir.UP_LEFT.vector ) ) );
        allowed.add( from.add( Dir.LEFT.vector.add( Dir.DOWN_LEFT.vector ) ) );
        allowed.add( from.add( Dir.UP.vector.add( Dir.UP_LEFT.vector ) ) );
        allowed.add( from.add( Dir.UP.vector.add( Dir.UP_RIGHT.vector ) ) );
        allowed.add( from.add( Dir.RIGHT.vector.add( Dir.UP_RIGHT.vector ) ) );
        allowed.add( from.add( Dir.RIGHT.vector.add( Dir.DOWN_RIGHT.vector ) ) );
        allowed.add( from.add( Dir.DOWN.vector.add( Dir.DOWN_LEFT.vector ) ) );
        allowed.add( from.add( Dir.DOWN.vector.add( Dir.DOWN_RIGHT.vector ) ) );

        allowed = allowed.stream()
                .filter( p -> !game.isOutOfBounds( p ) )
                .collect( Collectors.toSet() );
        return allowed;
    }

    public static Set<Vector2I> generateBishopMoves( Game game, Vector2I from ) {

        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.BISHOP ) ) {
            return allowed;
        }

        Dir.diagonalDirs().forEach( dir -> allowed.addAll(
                getPositionsOfDir( game, from, dir.vector, -1, false, false, true, true )
        ) );
        return allowed;
    }

    public static Set<Vector2I> generateBishopAttackingMoves( Game game, Vector2I from ) {

        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.BISHOP ) ) {
            return allowed;
        }

        Dir.diagonalDirs().forEach( dir -> allowed.addAll(
                getPositionsOfDir( game, from, dir.vector, -1, false, true, true, true )
        ) );
        return allowed;
    }

    public static Set<Vector2I> generateRookMoves( Game game, Vector2I from ) {

        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.ROOK ) ) {
            return allowed;
        }

        Dir.baseDirs().forEach( dir -> allowed.addAll(
                getPositionsOfDir( game, from, dir.vector, -1, false, false, true, true )
        ) );
        return allowed;
    }

    public static Set<Vector2I> generateRookAttackingMoves( Game game, Vector2I from ) {

        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.ROOK ) ) {
            return allowed;
        }

        Dir.baseDirs().forEach( dir -> allowed.addAll(
                getPositionsOfDir( game, from, dir.vector, -1, false, true, true, true )
        ) );
        return allowed;
    }

    public static Set<Vector2I> generateQueenMoves( Game game, Vector2I from ) {

        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.QUEEN ) ) {
            return allowed;
        }

        Arrays.stream( Dir.values() ).forEach( dir -> allowed.addAll(
                getPositionsOfDir( game, from, dir.vector, -1, false, false, true, true )
        ) );
        return allowed;
    }

    public static Set<Vector2I> generateQueenAttackingMoves( Game game, Vector2I from ) {

        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.QUEEN ) ) {
            return allowed;
        }

        Arrays.stream( Dir.values() ).forEach( dir -> allowed.addAll(
                getPositionsOfDir( game, from, dir.vector, -1, false, true, true, true )
        ) );
        return allowed;
    }

    public static Set<Vector2I> generateKingMoves( Game game, Vector2I from ) {

        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.KING ) ) {
            return allowed;
        }

        Arrays.stream( Dir.values() ).forEach( dir -> allowed.addAll(
                getPositionsOfDir( game, from, dir.vector, 1, false, false, true, true )
        ) );
        return allowed.stream().filter( p -> !game.isAttacked( p ) ).collect( Collectors.toSet() );
    }

    public static Set<Vector2I> generateCastleQueenMoves( Game game, Vector2I from ) {

        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.KING ) ) {
            return allowed;
        }

        // check king
        Vector2I kingPos = game.isTeam( from, TeamColor.WHITE ) ? new Vector2I( 4, 0 ) : new Vector2I( 4, game.getBoardSize() - 1 );
        if ( !from.equals( kingPos ) || game.isAttacked( kingPos ) ) {
            return allowed;
        }

        // check rook
        Vector2I rookPos = game.isTeam( from, TeamColor.WHITE ) ? new Vector2I( 0, 0 ) : new Vector2I( 0, game.getBoardSize() - 1 );
        if ( !game.isType( rookPos, PieceType.ROOK ) ) {
            return allowed;
        }

        Vector2I target = from.add( Dir.LEFT.vector.mul( 2 ) );
        // check in between
        List<Vector2I> inBetween = getPositionsOfDir( game, rookPos, Dir.RIGHT.vector, -1, false, false, false, false );
        if ( inBetween.size() != kingPos.x - 1 ) {
            return allowed;
        }
        boolean foundTarget = false;
        for ( int i = inBetween.size() - 1; i >= 0; i-- ) {
            Vector2I p = inBetween.get( i );
            if ( !foundTarget ) {
                if ( game.isAttacked( p ) ) {
                    return allowed;
                }
            }
            if ( p.equals( target ) ) {
                foundTarget = true;
            }
        }

        allowed.add( target );
        return allowed;
    }

    public static Set<Vector2I> generateCastleKingMoves( Game game, Vector2I from ) {

        Set<Vector2I> allowed = new HashSet<>();
        if ( !game.isType( from, PieceType.KING ) ) {
            return allowed;
        }

        // check king
        Vector2I kingPos = game.isTeam( from, TeamColor.WHITE ) ? new Vector2I( 4, 0 ) : new Vector2I( 4, game.getBoardSize() - 1 );
        if ( !from.equals( kingPos ) || game.isAttacked( kingPos ) ) {
            return allowed;
        }

        // check rook
        Vector2I rookPos = game.isTeam( from, TeamColor.WHITE ) ? new Vector2I( game.getBoardSize() - 1, 0 ) : new Vector2I( game.getBoardSize() - 1, game.getBoardSize() - 1 );
        if ( !game.isType( rookPos, PieceType.ROOK ) ) {
            return allowed;
        }

        Vector2I target = from.add( Dir.RIGHT.vector.mul( 2 ) );
        // check in between
        List<Vector2I> inBetween = getPositionsOfDir( game, rookPos, Dir.LEFT.vector, -1, false, false, false, false );
        if ( inBetween.size() != game.getBoardSize() - kingPos.x - 2 ) {
            return allowed;
        }
        boolean foundTarget = false;
        for ( int i = inBetween.size() - 1; i >= 0; i-- ) {
            Vector2I p = inBetween.get( i );
            if ( !foundTarget ) {
                if ( game.isAttacked( p ) ) {
                    return allowed;
                }
            }
            if ( p.equals( target ) ) {
                foundTarget = true;
            }
        }

        allowed.add( target );
        return allowed;
    }

    public static List<Vector2I> getPositionsOfDir( Game game, Vector2I from, Vector2I dir, int distance, boolean ignorePieces, boolean ignoreEnemyKing, boolean includeEnemyContact, boolean includeTeamContact ) {

        if ( distance < 0 ) {
            distance = game.getBoardSize() * game.getBoardSize();
        }

        List<Vector2I> positions = new ArrayList<>();
        for ( int i = 0; i < distance; i++ ) {

            Vector2I p = from.add( dir.mul( i + 1 ) );

            if ( !game.isOutOfBounds( p ) ) {

                if ( !ignorePieces && game.hasPiece( p ) && ( !ignoreEnemyKing || !game.areEnemies( p, from ) || !game.isType( p, PieceType.KING ) ) ) {

                    if ( includeEnemyContact && game.areEnemies( p, from ) ) {
                        positions.add( p );
                    } else if ( includeTeamContact && !game.areEnemies( p, from ) ) {
                        positions.add( p );
                    }

                    // position is occupied
                    return positions;
                }

                positions.add( p );

            }

        }

        return positions;
    }

}
