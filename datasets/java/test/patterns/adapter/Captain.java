package de.haw.example.adapter;

public class Captain {

    private final RowingBoat rowingBoat;

    public Captain( RowingBoat rowingBoat ) {
        this.rowingBoat = rowingBoat;
    }

    public void row() {
        rowingBoat.row();
    }
}
