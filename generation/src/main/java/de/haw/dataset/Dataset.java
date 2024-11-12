package de.haw.dataset;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Dataset {
    SELF( "generation" ),
    ANIMAL( "animals" ),
    CHESS( "chess" ),
    QUICK_UML( "1 - QuickUML 2001" ),
    LEXI( "2 - Lexi v0.1.1 alpha" ),
    J_REFACTORY( "3 - JRefactory v2.6.24" ),
    NETBEANS( "4 - Netbeans v1.0.x" ),
    J_UNIT( "5 - JUnit v3.7" ),
    J_HOT_DRAW( "6 - JHotDraw v5.1" ),
    MAPPER_XML( "8 - MapperXML v1.9.7" ),
    NUTCH( "10 - Nutch v0.4" ),
    PMD( "11 - PMD v1.8" );

    private final String name;
}
