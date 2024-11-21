package de.haw.example.builder;

import de.haw.example.builder.types.*;

public class Main {

    public static void main( String[] args ) {

        var mage = new Hero.Builder( Profession.MAGE, "Riobard" )
                .withHairColor( HairColor.BLACK )
                .withWeapon( Weapon.DAGGER )
                .build();
        System.out.println( mage.toString() );

        var warrior = new Hero.Builder( Profession.WARRIOR, "Amberjill" )
                .withHairColor( HairColor.BLOND )
                .withHairType( HairType.LONG_CURLY )
                .withArmor( Armor.CHAIN_MAIL )
                .withWeapon( Weapon.SWORD )
                .build();
        System.out.println( warrior.toString() );

        var thief = new Hero.Builder( Profession.THIEF, "Desmond" )
                .withHairType( HairType.BALD )
                .withWeapon( Weapon.BOW )
                .build();
        System.out.println( thief.toString() );
    }

}
