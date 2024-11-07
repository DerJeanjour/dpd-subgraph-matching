package de.haw.processing;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CpgPipe implements CpgProcessor {

    private final List<CpgProcessor> processors;

    @Override
    public Object process( final Object input ) {
        Object data = input;
        for ( final CpgProcessor processor : this.processors ) {
            log.info( "Processing module [{}] ...", processor.getClass().getSimpleName() );
            data = processor.process( data );
        }
        return data;
    }

    public <Input, Output> Output getResult( final Input input ) {
        return ( Output ) this.process( input );
    }

    @NoArgsConstructor( staticName = "builder" )
    public static class Builder {

        private final List<CpgProcessor> processors = new ArrayList<>();

        public Builder add( final CpgProcessor processor ) {
            this.processors.add( processor );
            return this;
        }

        public CpgPipe build() {
            return new CpgPipe( this.processors );
        }

    }

}
