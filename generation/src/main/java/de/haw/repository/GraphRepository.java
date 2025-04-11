package de.haw.repository;

import de.haw.misc.Args;
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
    private final String DEFAULT_PORT = "7687";
    private final String DEFAULT_USER_NAME = "neo4j";
    private final String DEFAULT_PASSWORD = "password";

    private final String[] PACKAGES = { "de.haw" };

    private final SessionFactory sessionFactory;

    public GraphRepository( final Args args ) {
        final String protocol = args.getOrElse( "neo4j-protocol", PROTOCOL );
        final String host = args.getOrElse( "neo4j-host", DEFAULT_HOST );
        final String port = args.getOrElse( "neo4j-port", DEFAULT_PORT );
        final String user = args.getOrElse( "neo4j-user", DEFAULT_USER_NAME );
        final String pw = args.getOrElse( "neo4j-pw", DEFAULT_PASSWORD );
        Configuration configuration = new Configuration.Builder().uri( protocol + host + port )
                .credentials( user, pw )
                .verifyConnection( VERIFY_CONNECTION )
                .build();
        this.sessionFactory = new SessionFactory( configuration, PACKAGES );
    }

    public static GraphRepository instance() {
        return new GraphRepository( Args.empty() );
    }

    public static GraphRepository instance( final Args args ) {
        return new GraphRepository( args );
    }

    @Override
    public void close() {
        this.sessionFactory.close();
    }

    public void clearAll() {
        log.info( "Deleting repository data ..." );
        Session session = sessionFactory.openSession();
        try ( Transaction tx = session.beginTransaction() ) {
            session.purgeDatabase();
            tx.commit();
        }
    }

    public void writeGraph( final Graph graph, final int depth ) {
        log.info( "Writing graph to repository ..." );
        Session session = sessionFactory.openSession();
        try ( Transaction tx = session.beginTransaction() ) {
            final List<CpgEdge<CpgNode>> cpgEdges = GraphMapper.map( graph );
            session.save( cpgEdges, depth );
            tx.commit();
        }
    }


}
