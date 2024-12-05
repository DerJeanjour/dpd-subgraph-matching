package de.haw.repository.model;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.*;

import java.util.*;

@Getter
@Setter
@NodeEntity
public class CpgNode {

    @Id
    @GeneratedValue
    private Long id;

    @Labels
    private Collection<String> labels;

    @Properties( prefix = "node", allowCast = false )
    private Map<String, String> properties = new HashMap<>();

    /* OWN RELATIONSHIPS */

    @Relationship( value = "RECORD_KNOWS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> nextKnownRecords = new ArrayList<>();

    /* RELATIONSHIPS */

    @Relationship( value = "AST", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> nextAstEdges = new ArrayList<>();

    @Relationship( value = "EOG", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> nextEogEdge = new ArrayList<>();

    @Relationship( value = "CDG", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> nextCdgEdge = new ArrayList<>();

    @Relationship( value = "DFG", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> nextDfgEdge = new ArrayList<>();

    @Relationship( value = "PDG", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> nextPdgEdge = new ArrayList<>();

    @Relationship( value = "SCOPE", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> scope = new ArrayList<>();


    /* OTHER RELATIONSHIPS */

    @Relationship( value = "ANONYMOUS_CLASS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> anonymousClass = new ArrayList<>();

    @Relationship( value = "ARGUMENTS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> arguments = new ArrayList<>();

    @Relationship( value = "ARRAY_EXPRESSION", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> arrayExpression = new ArrayList<>();

    @Relationship( value = "ASSIGNED_TYPES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> assignedTypes = new ArrayList<>();

    @Relationship( value = "AST_NODE", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> astNode = new ArrayList<>();

    @Relationship( value = "BASE", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> base = new ArrayList<>();

    @Relationship( value = "BODY", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> body = new ArrayList<>();

    @Relationship( value = "BREAKS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> breaks = new ArrayList<>();

    @Relationship( value = "CALLEE", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> callee = new ArrayList<>();

    @Relationship( value = "CANDIDATES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> candidates = new ArrayList<>();

    @Relationship( value = "CAST_TYPE", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> castType = new ArrayList<>();

    @Relationship( value = "CATCH_CLAUSES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> catchClauses = new ArrayList<>();

    @Relationship( value = "CONDITION", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> condition = new ArrayList<>();

    @Relationship( value = "CONSTRUCTOR", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> constructor = new ArrayList<>();

    @Relationship( value = "CONSTRUCTORS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> constructors = new ArrayList<>();

    @Relationship( value = "DECLARATIONS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> declarations = new ArrayList<>();

    @Relationship( value = "DEFINES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> defines = new ArrayList<>();

    @Relationship( value = "DIMENSIONS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> dimensions = new ArrayList<>();

    @Relationship( value = "ELEMENT_TYPE", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> elementType = new ArrayList<>();

    @Relationship( value = "ELSE_EXPRESSION", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> elseExpression = new ArrayList<>();

    @Relationship( value = "ELSE_STATEMENT", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> elseStatement = new ArrayList<>();

    @Relationship( value = "EXPRESSION", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> expression = new ArrayList<>();

    @Relationship( value = "FIELDS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> fields = new ArrayList<>();

    @Relationship( value = "FINALLY_BLOCK", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> finallyBlock = new ArrayList<>();

    @Relationship( value = "GENERICS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> generics = new ArrayList<>();

    @Relationship( value = "IMPORTS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> imports = new ArrayList<>();

    @Relationship( value = "INCLUDES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> includes = new ArrayList<>();

    @Relationship( value = "INITIALIZER", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> initializer = new ArrayList<>();

    @Relationship( value = "INITIALIZER_STATEMENT", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> initializerStatement = new ArrayList<>();

    @Relationship( value = "INPUT", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> input = new ArrayList<>();

    @Relationship( value = "INSTANTIATES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> instantiates = new ArrayList<>();

    @Relationship( value = "INVOKES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> invokes = new ArrayList<>();

    @Relationship( value = "ITERATION_STATEMENT", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> iterationStatement = new ArrayList<>();

    @Relationship( value = "LANGUAGE", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> language = new ArrayList<>();

    @Relationship( value = "LHS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> lhs = new ArrayList<>();

    @Relationship( value = "LOCALS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> locals = new ArrayList<>();

    @Relationship( value = "METHODS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> methods = new ArrayList<>();

    @Relationship( value = "NAMESPACES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> namespaces = new ArrayList<>();

    @Relationship( value = "OVERRIDES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> overrides = new ArrayList<>();

    @Relationship( value = "PARAMETER", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> parameter = new ArrayList<>();

    @Relationship( value = "PARAMETERS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> parameters = new ArrayList<>();

    @Relationship( value = "PARENT", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> parent = new ArrayList<>();

    @Relationship( value = "RECEIVER", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> receiver = new ArrayList<>();

    @Relationship( value = "RECORDS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> records = new ArrayList<>();

    @Relationship( value = "RECORD_DECLARATION", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> recordDeclaration = new ArrayList<>();

    @Relationship( value = "REFERS_TO", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> refersTo = new ArrayList<>();

    @Relationship( value = "RESOLUTION_HELPER", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> resolutionHelper = new ArrayList<>();

    @Relationship( value = "RETURN_TYPES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> returnTypes = new ArrayList<>();

    @Relationship( value = "RETURN_VALUES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> returnValues = new ArrayList<>();

    @Relationship( value = "RHS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> rhs = new ArrayList<>();

    @Relationship( value = "STATEMENT", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> statement = new ArrayList<>();

    @Relationship( value = "STATEMENTS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> statements = new ArrayList<>();

    @Relationship( value = "SUBSCRIPT_EXPRESSION", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> subscriptExpression = new ArrayList<>();

    @Relationship( value = "SUPER_TYPE", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> superType = new ArrayList<>();

    @Relationship( value = "SUPER_TYPE_DECLARATIONS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> superTypeDeclarations = new ArrayList<>();

    @Relationship( value = "THEN_EXPRESSION", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> thenExpression = new ArrayList<>();

    @Relationship( value = "THEN_STATEMENT", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> thenStatement = new ArrayList<>();

    @Relationship( value = "THROWS_TYPES", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> throwsTypes = new ArrayList<>();

    @Relationship( value = "TRANSLATION_UNITS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> translationUnits = new ArrayList<>();

    @Relationship( value = "TRY_BLOCK", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> tryBlock = new ArrayList<>();

    @Relationship( value = "TYPE", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> type = new ArrayList<>();

    @Relationship( value = "TYPE_OBSERVERS", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> typeObservers = new ArrayList<>();

    @Relationship( value = "USAGE", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> usage = new ArrayList<>();

}
