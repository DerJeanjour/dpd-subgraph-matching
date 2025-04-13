package de.haw.misc.pipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PipeBuilderTest {

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

    @Test
    void testBuildWithoutModulesThrowsException() {
        PipeBuilder<String, String> builder = PipeBuilder.builder();
        IllegalStateException ex = assertThrows( IllegalStateException.class, builder::build );
        assertEquals( "Pipe must have at least 1 module.", ex.getMessage() );
    }

    @Test
    void testBuildSingleModule() {
        PipeBuilder<String, String> builder = PipeBuilder.builder();
        DummeyModuleA<String> moduleA = new DummeyModuleA<>();
        builder.add( moduleA );
        PipeModule<String, ?, String> pipe = builder.build();
        // Expect the built pipe to be the single module we added.
        assertSame( moduleA, pipe );
    }

    @Test
    void testBuildChainedModules() {
        PipeBuilder<String, String> builder = PipeBuilder.builder();
        DummeyModuleA<String> moduleA = new DummeyModuleA<>();
        DummeyModuleB<String> moduleB = new DummeyModuleB<>();
        builder.add( moduleA ).add( moduleB );
        PipeModule<String, ?, String> pipe = builder.build();
        // The head of the chain should be moduleA.
        assertSame( moduleA, pipe );
        // Verify that moduleA's next module is moduleB.
        assertSame( moduleB, moduleA.getNext() );
    }

}
