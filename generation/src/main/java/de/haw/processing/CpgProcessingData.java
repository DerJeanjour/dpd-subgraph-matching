package de.haw.processing;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor( staticName = "of" )
public class CpgProcessingData<T> {

    private final T data;

}
