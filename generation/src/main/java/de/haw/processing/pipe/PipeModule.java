package de.haw.processing.pipe;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter( AccessLevel.PROTECTED )
@Setter( AccessLevel.PROTECTED )
public abstract class PipeModule<Input, Output, Target> {

    private PipeModule<Output, ?, Target> next;

    @SuppressWarnings( "unchecked" )
    public Target process( final Input input ) {
        final Output output = this.processImpl( input );
        if ( this.next == null ) {
            return ( Target ) output;
        }
        return this.next.process( output );
    }

    protected abstract Output processImpl( Input input );

}
