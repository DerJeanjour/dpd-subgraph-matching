package de.haw.dataset.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleClassType {
    CLASS( "Class" ),
    ABSTRACT_CLASS( "AbstractClass" );

    private final String name;
}
