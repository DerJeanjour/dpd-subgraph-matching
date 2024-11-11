package de.haw.repository.model;

import lombok.Data;
import org.neo4j.ogm.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Data
@RelationshipEntity
public class CpgEdge<T extends CpgNode> {

    @Id
    @GeneratedValue
    private Long id;

    @StartNode
    private T source;

    @EndNode
    private T target;

    @Properties( prefix = "edge", allowCast = false )
    private Map<String, String> properties = new HashMap<>();

}
