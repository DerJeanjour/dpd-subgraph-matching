package de.haw.repository.model;

import lombok.Data;

import java.util.List;

@Data
public class Neo4jGraph {

    private List<Neo4jNode> nodes;

    private List<Neo4jEdge> edges;

}
