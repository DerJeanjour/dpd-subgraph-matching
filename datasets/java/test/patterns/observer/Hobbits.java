package de.haw.example.observer;

public class Hobbits implements WeatherObserver {

    @Override
    public void update( WeatherType currentWeather ) {
        System.out.println( "The hobbits are facing " + currentWeather.getDescription() + " weather now" );
    }
}
