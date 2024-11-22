package de.haw.translation.module;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg_vis_neo4j.Application;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.misc.utils.ReflectionUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class PersistTranslationModule<Target> extends PipeModule<TranslationResult, TranslationResult, Target> {

    private final static String NEO4J_DEFAULT_USER = "neo4j";

    private final static String NEO4J_DEFAULT_PASSWORD = "password";

    @Override
    protected TranslationResult processImpl( final TranslationResult result, final PipeContext ctx ) {

        log.info( "Persisting parsed results ..." );
        if ( result == null ) {
            throw new IllegalArgumentException( "Can't persist null result!" );
        }

        final Application neo4j = new Application();
        neo4j.setNeo4jUsername( NEO4J_DEFAULT_USER );
        neo4j.setNeo4jPassword( NEO4J_DEFAULT_PASSWORD );
        ReflectionUtils.setInt( neo4j, "depth", ctx.get( PipeContext.CPG_DEPTH_KEY, 10, Integer.class ) );

        try {
            neo4j.pushToNeo4j( result );
        } catch ( InterruptedException | ConnectException e ) {
            throw new RuntimeException( e );
        }

        return result;
    }

}
