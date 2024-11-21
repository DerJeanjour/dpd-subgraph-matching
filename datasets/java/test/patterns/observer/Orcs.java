package de.haw.example.observer;

public class Orcs implements WeatherObserver {

    @Override
    public void update( WeatherType currentWeather ) {
        System.out.println( "The orcs are facing " + currentWeather.getDescription() + " weather now" );
    }
}
