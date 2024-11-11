package de.haw.misc;

import de.haw.misc.utils.FormatUtils;

public class Timer {

    private long start;

    public Timer() {
        reset();
    }

    public long getTimeSinceMillis() {
        return System.currentTimeMillis() - this.start;
    }

    public double getTimeSinceSec() {
        long timeSince = getTimeSinceMillis();
        return timeSince / 1000d;
    }

    public double getTimeSinceMin() {
        double timeSince = getTimeSinceSec();
        return timeSince / 60d;
    }

    public double getTimeSinceHour() {
        double timeSince = getTimeSinceMin();
        return timeSince / 60d;
    }

    public String getTimeSince() {
        return FormatUtils.format( getTimeSinceSec(), 2 );
    }

    public void reset() {
        this.start = System.currentTimeMillis();
    }

}
