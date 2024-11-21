package de.haw.example.singleton;

public class Main {

    public static void main( String[] args ) {
        JokerInstance jokerA = JokerInstance.getInstance();
        jokerA.showMessage();

        JokerInstance jokerB = JokerInstance.getInstance();
        jokerB.showMessage();

        System.out.println( "Joker A is Joker B: " + ( jokerA == jokerB ) );
    }

}
