package de.haw.example.adapter;

public class Main {

    public static void main( final String[] args ) {
        var captain = new Captain( new FishingBoatAdapter() );
        captain.row();
    }

}
