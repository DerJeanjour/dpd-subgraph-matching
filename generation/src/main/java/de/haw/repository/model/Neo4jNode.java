package de.haw.repository.model;

import lombok.Data;
import org.neo4j.ogm.annotation.Labels;
import org.neo4j.ogm.annotation.NodeEntity;

import java.util.List;

@Data
@NodeEntity
public class Neo4jNode extends Neo4jEntity {

    @Labels
    private List<String> labels;

    /*
    @Relationship( type = "REL", direction = Relationship.Direction.OUTGOING )
    private List<Neo4jEdge> edges;
     */

}
