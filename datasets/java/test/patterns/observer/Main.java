package de.haw.example.observer;

public class Main {

    public static void main( String[] args ) {

        var weather = new Weather();
        weather.addObserver( new Orcs() );
        weather.addObserver( new Hobbits() );

        weather.timePasses();
        weather.timePasses();
        weather.timePasses();
        weather.timePasses();
    }

}
