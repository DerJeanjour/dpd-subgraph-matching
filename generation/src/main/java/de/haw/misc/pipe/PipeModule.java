package de.haw.misc.pipe;

import de.haw.misc.Timer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter( AccessLevel.PROTECTED )
@Setter( AccessLevel.PROTECTED )
public abstract class PipeModule<Input, Output, Target> {

    private PipeModule<Output, ?, Target> next;

    @SuppressWarnings( "unchecked" )
    public Target process( final Input input, final PipeContext ctx ) {

        log.info( "---- STARTING [{}] ----", this.getModuleName() );
        final Timer timer = new Timer();
        final Output output = this.processImpl( input, ctx );
        log.info( "---- PROCESSED [{}] in {}s ----", this.getModuleName(), timer.getTimeSince() );

        if ( this.next == null ) {
            return ( Target ) output;
        }
        return this.next.process( output, ctx );
    }

    protected abstract Output processImpl( Input input, final PipeContext ctx );

    private String getModuleName() {
        return this.getClass().getSimpleName();
    }
}
