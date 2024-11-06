package de.haw.processing.processor;

public interface CpgProcessor<Input, Output> {

    Output process( Input input );

}
