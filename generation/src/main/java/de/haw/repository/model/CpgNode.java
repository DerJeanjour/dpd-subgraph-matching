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

    /* RELATIONSHIPS */

    @Relationship( value = "SCOPE", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> scope = new ArrayList<>();

    @Relationship( value = "AST", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> nextAstEdges = new ArrayList<>();

    @Relationship( value = "EOG", direction = Relationship.Direction.INCOMING )
    private List<CpgEdge<CpgNode>> prevEogEdges = new ArrayList<>();

    @Relationship( value = "EOG", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> nextEogEdge = new ArrayList<>();

    @Relationship( value = "CDG", direction = Relationship.Direction.INCOMING )
    private List<CpgEdge<CpgNode>> prevCdgEdges = new ArrayList<>();

    @Relationship( value = "CDG", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> nextCdgEdge = new ArrayList<>();

    @Relationship( value = "DFG", direction = Relationship.Direction.INCOMING )
    private List<CpgEdge<CpgNode>> prevDfgEdges = new ArrayList<>();

    @Relationship( value = "DFG", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> nextDfgEdge = new ArrayList<>();

    @Relationship( value = "PDG", direction = Relationship.Direction.INCOMING )
    private List<CpgEdge<CpgNode>> prevPdgEdges = new ArrayList<>();

    @Relationship( value = "PDG", direction = Relationship.Direction.OUTGOING )
    private List<CpgEdge<CpgNode>> nextPdgEdge = new ArrayList<>();

}
