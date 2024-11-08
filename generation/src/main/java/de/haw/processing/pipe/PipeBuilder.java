package de.haw.processing.pipe;

import lombok.NoArgsConstructor;

@NoArgsConstructor( staticName = "builder" )
public class PipeBuilder<Input, Output> {

    private PipeModule<?, ?, Output> head;
    private PipeModule<?, ?, Output> tail;

    @SuppressWarnings( "unchecked" )
    public <Module_Input, Module_Output> PipeBuilder<Input, Output> add(
            final PipeModule<Module_Input, Module_Output, Output> module ) {
        if ( this.head == null ) {
            this.head = module;
        } else {
            ( ( PipeModule<?, Module_Input, Output> ) this.tail ).setNext( module );
        }
        this.tail = module;
        return this;
    }

    @SuppressWarnings( "unchecked" )
    public PipeModule<Input, ?, Output> build() {
        if ( this.head == null ) {
            throw new IllegalStateException( "Pipe must have at least 1 module." );
        }
        return ( PipeModule<Input, ?, Output> ) this.head;
    }

}
