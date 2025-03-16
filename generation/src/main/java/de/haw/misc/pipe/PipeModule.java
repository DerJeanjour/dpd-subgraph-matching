package de.haw.misc.pipe;

import de.haw.misc.Timer;
import de.haw.misc.utils.FormatUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

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

        final int processCount = ctx.get( PipeContext.PROCESS_COUNT, 0, Integer.class );
        ctx.set( PipeContext.PROCESS_COUNT, processCount + 1 );

        final double processTime = timer.getTimeSinceSec();
        final double processTimeTotal = ctx.get( PipeContext.TOTAL_PROCESSING_TIME, 0d, Double.class ) + processTime;
        ctx.set( PipeContext.TOTAL_PROCESSING_TIME, processTimeTotal );

        final PipeBenchmark benchmark = PipeBenchmark.builder()
                .name( ctx.get( PipeContext.PROCESS_NAME ) )
                .processCount( processCount )
                .processName( this.getModuleName() )
                .processTimeSec( FormatUtils.format( processTime, 4 ) )
                .totalTimeSec( FormatUtils.format( processTimeTotal, 4 ) )
                .build();

        final List<PipeBenchmark> benchmarks = ctx.get( PipeContext.PIPE_BENCHMARKS, new ArrayList<>(), List.class );
        benchmarks.add( benchmark );
        ctx.set( PipeContext.PIPE_BENCHMARKS, benchmarks );

        log.info( "---- PROCESSED [{}] in {}s (total: {}s) ----", benchmark.getProcessName(),
                benchmark.getProcessTimeSec(), benchmark.getTotalTimeSec() );

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
