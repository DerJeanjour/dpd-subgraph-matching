package de.haw.dataset.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor( staticName = "of" )
public class DesignPattern {

    private final DesignPatterType type;

    private final String className;

}
