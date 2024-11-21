package de.haw.example.singleton;

public class JokerInstance {

    private static JokerInstance instance;

    private JokerInstance() {
        System.out.println( "Joker instance created." );
    }

    public static JokerInstance getInstance() {
        if ( instance == null ) {
            instance = new JokerInstance();
        }
        return instance;
    }

    public void showMessage() {
        System.out.println( "Hello from Joker!" );
    }

}
