package de.haw.dataset.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor( staticName = "of" )
public class DesignPattern {

    private final DesignPatternType type;

    private final String className;

}
