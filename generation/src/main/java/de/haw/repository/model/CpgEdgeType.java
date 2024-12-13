package de.haw.repository.model;

import java.util.Arrays;
import java.util.List;

public enum CpgEdgeType {

    // Interactions
    INTERACTS,

    // Aggregated
    AST,
    CDG,
    DFG,
    EOG,
    PDG,

    // Other
    ANONYMOUS_CLASS,
    ARGUMENTS,
    ARRAY_EXPRESSION,
    ASSIGNED_TYPES,
    AST_NODE,
    BASE,
    BODY,
    BREAKS,
    CALLEE,
    CANDIDATES,
    CAST_TYPE,
    CATCH_CLAUSES,
    CONDITION,
    CONSTRUCTOR,
    CONSTRUCTORS,
    DECLARATIONS,
    DEFINES,
    DIMENSIONS,
    ELEMENT_TYPE,
    ELSE_EXPRESSION,
    ELSE_STATEMENT,
    EXPRESSION,
    FIELDS,
    FINALLY_BLOCK,
    GENERICS,
    IMPORTS,
    INCLUDES,
    INITIALIZER,
    INITIALIZER_STATEMENT,
    INPUT,
    INSTANTIATES,
    INVOKES,
    ITERATION_STATEMENT,
    LANGUAGE,
    LHS,
    LOCALS,
    METHODS,
    NAMESPACES,
    OVERRIDES,
    PARAMETER,
    PARAMETERS,
    PARENT,
    RECEIVER,
    RECORDS,
    RECORD_DECLARATION,
    REFERS_TO,
    RESOLUTION_HELPER,
    RETURN_TYPES,
    RETURN_VALUES,
    RHS,
    SCOPE,
    STATEMENT,
    STATEMENTS,
    SUBSCRIPT_EXPRESSION,
    SUPER_TYPE,
    SUPER_TYPE_DECLARATIONS,
    THEN_EXPRESSION,
    THEN_STATEMENT,
    THROWS_TYPES,
    TRANSLATION_UNITS,
    TRY_BLOCK,
    TYPE,
    TYPE_OBSERVERS,
    USAGE;

    public static final List<CpgEdgeType> ALL = Arrays.asList( values() );

    public static final List<CpgEdgeType> MAIN = Arrays.asList( AST, CDG, DFG, EOG, PDG );

    public static final List<CpgEdgeType> OWN = Arrays.asList( INTERACTS );

}
