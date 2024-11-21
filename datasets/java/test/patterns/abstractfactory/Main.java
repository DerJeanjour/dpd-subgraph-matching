package de.haw.example.abstractfactory;


public class Main {

    public static void main( String[] args ) {

        KingdomFactory elfFactory = FactoryMaker.makeFactory( FactoryMaker.KingdomType.ELF );
        Castle elfCastle = elfFactory.createCastle();
        System.out.println( "Castle: " + elfCastle.getDescription() );
        King elfKing = elfFactory.createKing();
        System.out.println( "King: " + elfKing.getDescription() );
        Army elfArmy = elfFactory.createArmy();
        System.out.println( "Army: " + elfArmy.getDescription() );

        KingdomFactory orcFactory = FactoryMaker.makeFactory( FactoryMaker.KingdomType.ORC );
        Castle orcCastle = orcFactory.createCastle();
        System.out.println( "Castle: " + orcCastle.getDescription() );
        King orcKing = orcFactory.createKing();
        System.out.println( "King: " + orcKing.getDescription() );
        Army orcArmy = orcFactory.createArmy();
        System.out.println( "Army: " + orcArmy.getDescription() );

    }

}
