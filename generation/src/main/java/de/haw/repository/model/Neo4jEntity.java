package de.haw.repository.model;

import lombok.Data;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Properties;

import java.util.Map;

@Data
public class Neo4jEntity {

    @Id
    //@GeneratedValue
    private String id;

    @Properties
    private Map<String, Object> properties;

}
