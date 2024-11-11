package de.haw.repository;

import de.haw.repository.model.CpgEdge;
import de.haw.repository.model.CpgNode;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;

import java.util.List;

@Slf4j
public class GraphRepository implements AutoCloseable {

    private final boolean VERIFY_CONNECTION = true;

    private final String PROTOCOL = "bolt://";

    private final String DEFAULT_HOST = "localhost:";
    private final int DEFAULT_PORT = 7687;
    private final String DEFAULT_USER_NAME = "neo4j";
    private final String DEFAULT_PASSWORD = "password";

    private final String[] PACKAGES = { "de.haw" };

    private final SessionFactory sessionFactory;

    public GraphRepository() {
        Configuration configuration = new Configuration.Builder().uri( PROTOCOL + DEFAULT_HOST + DEFAULT_PORT )
                .credentials( DEFAULT_USER_NAME, DEFAULT_PASSWORD )
                .verifyConnection( VERIFY_CONNECTION )
                .build();
        this.sessionFactory = new SessionFactory( configuration, PACKAGES );
    }

    public static GraphRepository instance() {
        return new GraphRepository();
    }

    @Override
    public void close() {
        this.sessionFactory.close();
    }

    public void writeGraph( final Graph graph, final int depth, final boolean purge ) {
        Session session = sessionFactory.openSession();
        try ( Transaction tx = session.beginTransaction() ) {
            if ( purge ) {
                session.purgeDatabase();
            }
            final List<CpgEdge<CpgNode>> cpgEdges = GraphMapper.map( graph );
            session.save( cpgEdges, depth );
            tx.commit();
        }
    }


}
