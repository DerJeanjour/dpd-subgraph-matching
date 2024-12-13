package de.haw.misc.pipe;

import de.haw.misc.Timer;
import de.haw.misc.utils.FormatUtils;
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

        final double processTime = timer.getTimeSinceSec();
        final double processTimeTotal = ctx.get( PipeContext.TOTAL_PROCESSING_TIME, 0d, Double.class ) + processTime;
        ctx.set( PipeContext.TOTAL_PROCESSING_TIME, processTimeTotal );
        log.info(
                "---- PROCESSED [{}] in {}s (total: {}s) ----", this.getModuleName(),
                FormatUtils.format( processTime, 2 ), FormatUtils.format( processTimeTotal, 2 ) );

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
