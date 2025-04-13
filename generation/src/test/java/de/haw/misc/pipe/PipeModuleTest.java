package de.haw.misc.pipe;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PipeModuleTest {

    private static class DummeyModuleA<Target> extends PipeModule<String, Integer, Target> {
        @Override
        protected Integer processImpl( final String v, final PipeContext ctx ) {
            return Integer.parseInt( v );
        }
    }

    private static class DummeyModuleB<Target> extends PipeModule<Integer, String, Target> {
        @Override
        protected String processImpl( final Integer v, final PipeContext ctx ) {
            return String.valueOf( v );
        }
    }

    private static class IdentityModule extends PipeModule<String, String, String> {
        @Override
        protected String processImpl( final String v, final PipeContext ctx ) {
            return v;
        }
    }

    @Test
    void testProcessSingleModule() {
        PipeContext ctx = PipeContext.empty();
        ctx.set( PipeContext.PROCESS_NAME, "Identity" );
        IdentityModule module = new IdentityModule();
        String result = module.process( "test", ctx );
        assertEquals( "test", result );

        Integer processCount = ctx.get( PipeContext.PROCESS_COUNT, 0, Integer.class );
        assertEquals( 1, processCount );

        List<?> benchmarks = ctx.get( PipeContext.PIPE_BENCHMARKS, new java.util.ArrayList<>(), List.class );
        assertEquals( 1, benchmarks.size() );
    }

    @Test
    void testProcessChainedModules() {
        PipeContext ctx = PipeContext.empty();
        ctx.set( PipeContext.PROCESS_NAME, "Chained" );
        DummeyModuleA<String> moduleA = new DummeyModuleA<>();
        DummeyModuleB<String> moduleB = new DummeyModuleB<>();
        moduleA.setNext( moduleB );

        String result = moduleA.process( "123", ctx );
        assertEquals( "123", result );

        Integer processCount = ctx.get( PipeContext.PROCESS_COUNT, 0, Integer.class );
        assertEquals( 2, processCount );

        List<?> benchmarks = ctx.get( PipeContext.PIPE_BENCHMARKS, new java.util.ArrayList<>(), List.class );
        assertEquals( 2, benchmarks.size() );

        Double totalTime = ctx.get( PipeContext.TOTAL_PROCESSING_TIME, 0d, Double.class );
        assertTrue( totalTime >= 0 );
    }
}
