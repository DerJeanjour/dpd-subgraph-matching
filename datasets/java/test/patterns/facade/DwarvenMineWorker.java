package de.haw.example.facade;

import java.util.Arrays;

public abstract class DwarvenMineWorker {

    public void goToSleep() {
        System.out.println( name() + " goes to sleep." );
    }

    public void wakeUp() {
        System.out.println( name() + " wakes up." );
    }

    public void goHome() {
        System.out.println( name() + " goes home." );
    }

    public void goToMine() {
        System.out.println( name() + " goes to the mine." );
    }

    private void action( Action action ) {
        switch ( action ) {
            case GO_TO_SLEEP -> goToSleep();
            case WAKE_UP -> wakeUp();
            case GO_HOME -> goHome();
            case GO_TO_MINE -> goToMine();
            case WORK -> work();
            default -> System.out.println( "Undefined action" );
        }
    }

    public void action( Action... actions ) {
        Arrays.stream( actions ).forEach( this::action );
    }

    public abstract void work();

    public abstract String name();

    enum Action {
        GO_TO_SLEEP,
        WAKE_UP,
        GO_HOME,
        GO_TO_MINE,
        WORK
    }
}
