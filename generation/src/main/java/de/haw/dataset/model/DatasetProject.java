package de.haw.dataset.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DatasetProject {

    SELF( DatasetType.OWN ),
    ANIMAL( DatasetType.OWN ),
    CHESS( DatasetType.OWN ),

    ABSTRACT_FACTORY_EXAMPLE( DatasetType.PATTERN_EXAMPLES ),
    SINGLETON_EXAMPLE( DatasetType.PATTERN_EXAMPLES ),

    QUICK_UML( DatasetType.P_MART ),
    LEXI( DatasetType.P_MART ),
    J_REFACTORY( DatasetType.P_MART ),
    NETBEANS( DatasetType.P_MART ),
    J_UNIT( DatasetType.P_MART ),
    J_HOT_DRAW( DatasetType.P_MART ),
    MAPPER_XML( DatasetType.P_MART ),
    NUTCH( DatasetType.P_MART ),
    PMD( DatasetType.P_MART );

    private final DatasetType type;

}
