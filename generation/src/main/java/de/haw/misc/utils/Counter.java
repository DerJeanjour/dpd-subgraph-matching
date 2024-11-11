package de.haw.misc.utils;

import java.util.concurrent.atomic.AtomicLong;

public class Counter {

    private AtomicLong counter;

    public Counter() {
        this.counter = new AtomicLong();
        reset();

    }

    public void reset() {
        this.counter.set( 0 );
    }

    public long add( int value ) {
        for ( int i = 0; i < value; i++ ) {
            increment();
        }
        return get();
    }

    public long increment() {
        return this.counter.incrementAndGet();
    }

    public long get() {
        return this.counter.get();
    }

}
