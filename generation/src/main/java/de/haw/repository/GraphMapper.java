package de.haw.repository;

import de.haw.repository.model.CpgEdgeType;
import de.haw.processing.GraphService;
import de.haw.repository.model.CpgEdge;
import de.haw.repository.model.CpgNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class GraphMapper {

    public static List<CpgEdge<CpgNode>> map( final Graph graph ) {
        final Map<Long, CpgNode> cpgNodes = map( graph.nodes() );
        final List<CpgEdge<CpgNode>> edges = graph.edges()
                .map( edge -> map( edge, cpgNodes ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
        cpgNodes.forEach( ( key, node ) -> node.setId( null ) );
        return edges;
    }

    private static Map<Long, CpgNode> map( final Stream<Node> nodes ) {
        final Map<Long, CpgNode> cpgNodes = new HashMap<>();
        nodes.forEach( node -> {
            final CpgNode cpgNode = map( node );
            cpgNodes.put( cpgNode.getId(), cpgNode );
        } );
        return cpgNodes;
    }

    @SuppressWarnings( "unchecked" )
    private static CpgNode map( final Node node ) {
        final CpgNode cpgNode = new CpgNode();
        cpgNode.setId( mapId( node ) );

        final Map<String, Object> attr = GraphService.instance().getAttributes( node );
        if ( attr.containsKey( "labels" ) ) {
            cpgNode.setLabels( ( Collection<String> ) node.getAttribute( "labels" ) );
        }

        cpgNode.setProperties( mapAttr( attr ) );

        return cpgNode;
    }

    private static CpgEdge<CpgNode> map( final Edge edge, final Map<Long, CpgNode> cpgNodes ) {
        final CpgEdge<CpgNode> cpgEdge = new CpgEdge<>();
        //cpgEdge.setId( mapId( edge ) );

        final Map<String, Object> attr = GraphService.instance().getAttributes( edge );
        cpgEdge.setProperties( mapAttr( attr ) );

        final Optional<CpgEdgeType> edgeType = mapEdgeType( edge );
        if ( edgeType.isEmpty() ) {
            return null;
        }

        final CpgNode cpgSourceNode = cpgNodes.get( mapId( edge.getSourceNode() ) );
        cpgEdge.setSource( cpgSourceNode );
        final CpgNode cpgTargetNode = cpgNodes.get( mapId( edge.getTargetNode() ) );
        cpgEdge.setTarget( cpgTargetNode );

        switch ( edgeType.get() ) {

            case AST -> cpgSourceNode.getNextAstEdges().add( cpgEdge );
            case DFG -> cpgSourceNode.getNextDfgEdge().add( cpgEdge );
            case EOG -> cpgSourceNode.getNextEogEdge().add( cpgEdge );
            case PDG -> cpgSourceNode.getNextPdgEdge().add( cpgEdge );
            case CDG -> cpgSourceNode.getNextCdgEdge().add( cpgEdge );
            case SCOPE -> cpgSourceNode.getScope().add( cpgEdge );

            case ANONYMOUS_CLASS -> cpgSourceNode.getAnonymousClass().add(cpgEdge);
            case ARGUMENTS -> cpgSourceNode.getArguments().add(cpgEdge);
            case ARRAY_EXPRESSION -> cpgSourceNode.getArrayExpression().add(cpgEdge);
            case ASSIGNED_TYPES -> cpgSourceNode.getAssignedTypes().add(cpgEdge);
            case AST_NODE -> cpgSourceNode.getAstNode().add(cpgEdge);
            case BASE -> cpgSourceNode.getBase().add(cpgEdge);
            case BODY -> cpgSourceNode.getBody().add(cpgEdge);
            case BREAKS -> cpgSourceNode.getBreaks().add(cpgEdge);
            case CALLEE -> cpgSourceNode.getCallee().add(cpgEdge);
            case CANDIDATES -> cpgSourceNode.getCandidates().add(cpgEdge);
            case CAST_TYPE -> cpgSourceNode.getCastType().add(cpgEdge);
            case CATCH_CLAUSES -> cpgSourceNode.getCatchClauses().add(cpgEdge);
            case CONDITION -> cpgSourceNode.getCondition().add(cpgEdge);
            case CONSTRUCTOR -> cpgSourceNode.getConstructor().add(cpgEdge);
            case CONSTRUCTORS -> cpgSourceNode.getConstructors().add(cpgEdge);
            case DECLARATIONS -> cpgSourceNode.getDeclarations().add(cpgEdge);
            case DEFINES -> cpgSourceNode.getDefines().add(cpgEdge);
            case DIMENSIONS -> cpgSourceNode.getDimensions().add(cpgEdge);
            case ELEMENT_TYPE -> cpgSourceNode.getElementType().add(cpgEdge);
            case ELSE_EXPRESSION -> cpgSourceNode.getElseExpression().add(cpgEdge);
            case ELSE_STATEMENT -> cpgSourceNode.getElseStatement().add(cpgEdge);
            case EXPRESSION -> cpgSourceNode.getExpression().add(cpgEdge);
            case FIELDS -> cpgSourceNode.getFields().add(cpgEdge);
            case FINALLY_BLOCK -> cpgSourceNode.getFinallyBlock().add(cpgEdge);
            case GENERICS -> cpgSourceNode.getGenerics().add(cpgEdge);
            case IMPORTS -> cpgSourceNode.getImports().add(cpgEdge);
            case INCLUDES -> cpgSourceNode.getIncludes().add(cpgEdge);
            case INITIALIZER -> cpgSourceNode.getInitializer().add(cpgEdge);
            case INITIALIZER_STATEMENT -> cpgSourceNode.getInitializerStatement().add(cpgEdge);
            case INPUT -> cpgSourceNode.getInput().add(cpgEdge);
            case INSTANTIATES -> cpgSourceNode.getInstantiates().add(cpgEdge);
            case INVOKES -> cpgSourceNode.getInvokes().add(cpgEdge);
            case ITERATION_STATEMENT -> cpgSourceNode.getIterationStatement().add(cpgEdge);
            case LANGUAGE -> cpgSourceNode.getLanguage().add(cpgEdge);
            case LHS -> cpgSourceNode.getLhs().add(cpgEdge);
            case LOCALS -> cpgSourceNode.getLocals().add(cpgEdge);
            case METHODS -> cpgSourceNode.getMethods().add(cpgEdge);
            case NAMESPACES -> cpgSourceNode.getNamespaces().add(cpgEdge);
            case OVERRIDES -> cpgSourceNode.getOverrides().add(cpgEdge);
            case PARAMETER -> cpgSourceNode.getParameter().add(cpgEdge);
            case PARAMETERS -> cpgSourceNode.getParameters().add(cpgEdge);
            case PARENT -> cpgSourceNode.getParent().add(cpgEdge);
            case RECEIVER -> cpgSourceNode.getReceiver().add(cpgEdge);
            case RECORDS -> cpgSourceNode.getRecords().add(cpgEdge);
            case RECORD_DECLARATION -> cpgSourceNode.getRecordDeclaration().add(cpgEdge);
            case REFERS_TO -> cpgSourceNode.getRefersTo().add(cpgEdge);
            case RESOLUTION_HELPER -> cpgSourceNode.getResolutionHelper().add(cpgEdge);
            case RETURN_TYPES -> cpgSourceNode.getReturnTypes().add(cpgEdge);
            case RETURN_VALUES -> cpgSourceNode.getReturnValues().add(cpgEdge);
            case RHS -> cpgSourceNode.getRhs().add(cpgEdge);
            case STATEMENT -> cpgSourceNode.getStatement().add(cpgEdge);
            case STATEMENTS -> cpgSourceNode.getStatements().add(cpgEdge);
            case SUBSCRIPT_EXPRESSION -> cpgSourceNode.getSubscriptExpression().add(cpgEdge);
            case SUPER_TYPE -> cpgSourceNode.getSuperType().add(cpgEdge);
            case SUPER_TYPE_DECLARATIONS -> cpgSourceNode.getSuperTypeDeclarations().add(cpgEdge);
            case THEN_EXPRESSION -> cpgSourceNode.getThenExpression().add(cpgEdge);
            case THEN_STATEMENT -> cpgSourceNode.getThenStatement().add(cpgEdge);
            case THROWS_TYPES -> cpgSourceNode.getThrowsTypes().add(cpgEdge);
            case TRANSLATION_UNITS -> cpgSourceNode.getTranslationUnits().add(cpgEdge);
            case TRY_BLOCK -> cpgSourceNode.getTryBlock().add(cpgEdge);
            case TYPE -> cpgSourceNode.getType().add(cpgEdge);
            case TYPE_OBSERVERS -> cpgSourceNode.getTypeObservers().add(cpgEdge);
            case USAGE -> cpgSourceNode.getUsage().add(cpgEdge);
        }

        return cpgEdge;
    }

    private static Optional<CpgEdgeType> mapEdgeType( final Edge edge ) {
        final String type = edge.getAttribute( "type", String.class );
        if ( StringUtils.isBlank( type ) ) {
            return Optional.empty();
        }
        for ( CpgEdgeType edgeType : CpgEdgeType.values() ) {
            if ( type.equals( edgeType.name() ) ) {
                return Optional.of( edgeType );
            }
        }
        return Optional.empty();
    }

    private static Long mapId( final Element element ) {
        return Long.valueOf( element.getId() );
    }

    public static Map<String, String> mapAttr( final Map<String, Object> originalMap ) {
        final Map<String, String> map = new HashMap<>();
        for ( Map.Entry<String, Object> entry : originalMap.entrySet() ) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if ( value != null && StringUtils.isNotBlank( value.toString() ) ) {
                map.put( key, value.toString() );
            }
        }
        return map;
    }

}
