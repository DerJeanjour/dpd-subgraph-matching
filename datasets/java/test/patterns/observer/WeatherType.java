package de.haw.example.observer;

public enum WeatherType {

    SUNNY( "Its sunnny." ),
    RAINY( "Its rainy." );

    private final String description;

    WeatherType( String description ) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

}
