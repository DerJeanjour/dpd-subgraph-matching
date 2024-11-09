package de.haw.repository.model;

import lombok.Data;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@Data
@RelationshipEntity // (type = "AST" ... )
public class Neo4jEdge extends Neo4jEntity {

    private String type;

    @StartNode
    private Neo4jNode source;

    @EndNode
    private Neo4jNode target;

}
