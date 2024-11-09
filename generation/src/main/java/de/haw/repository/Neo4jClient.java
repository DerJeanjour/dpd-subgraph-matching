package de.haw.repository;

import de.haw.repository.model.Neo4jGraph;
import org.graphstream.graph.Graph;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;

public class Neo4jClient implements AutoCloseable {

    private final boolean VERIFY_CONNECTION = true;

    private final String PROTOCOL = "bolt://";

    private final String DEFAULT_HOST = "localhost:";
    private final int DEFAULT_PORT = 7687;
    private final String DEFAULT_USER_NAME = "neo4j";
    private final String DEFAULT_PASSWORD = "password";

    private final String[] PACKAGES = { "de.haw" };

    private final SessionFactory sessionFactory;

    public Neo4jClient() {
        Configuration configuration = new Configuration.Builder().uri( PROTOCOL + DEFAULT_HOST + DEFAULT_PORT )
                .credentials( DEFAULT_USER_NAME, DEFAULT_PASSWORD )
                .verifyConnection( VERIFY_CONNECTION )
                .build();
        this.sessionFactory = new SessionFactory( configuration, PACKAGES );
    }

    @Override
    public void close() throws Exception {
        this.sessionFactory.close();
    }

    public void writeGraph( final Graph graph, final int depth, final boolean purge ) {
        Session session = sessionFactory.openSession();
        try ( Transaction tx = session.beginTransaction() ) {
            if ( purge ) {
                session.purgeDatabase();
            }
            final Neo4jGraph neoGraph = Neo4jMapper.map( graph );
            session.save( neoGraph.getNodes(), depth );
            session.save( neoGraph.getEdges(), depth );
            tx.commit();
        }
    }


}
