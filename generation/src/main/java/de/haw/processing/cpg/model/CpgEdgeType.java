package de.haw.processing.cpg.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CpgEdgeType {
    ABSTRACT_SYNTAX_TREE( "AST" ),
    DATA_FLOW_GRAPH( "DFG" ),
    EVALUATION_ORDER_GRAPH( "EOG" ),
    PROGRAM_DEPENDENCY_GRAPH( "PDG" ),
    CONTROL_DEPENDENCE_GRAPH( "CDG" ),
    CONTROL_FLOW_GRAPH( "CFG" );

    private final String value;

}
