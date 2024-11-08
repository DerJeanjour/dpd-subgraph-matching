package de.haw.processing.cpg.module;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg_vis_neo4j.Application;
import de.haw.processing.pipe.PipeModule;
import de.haw.utils.ReflectionUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class CpgPersistModule<Target> extends PipeModule<TranslationResult, TranslationResult, Target> {

    @Override
    protected TranslationResult processImpl( final TranslationResult result ) {

        log.info( "Persisting parsed results ..." );
        if ( result == null ) {
            throw new IllegalArgumentException( "Can't persist null result!" );
        }

        final Application neo4j = new Application();
        neo4j.setNeo4jUsername( "neo4j" );
        neo4j.setNeo4jPassword( "password" );
        ReflectionUtils.setInt( neo4j, "depth", 10 );

        try {
            neo4j.pushToNeo4j( result );
        } catch ( InterruptedException | ConnectException e ) {
            throw new RuntimeException( e );
        }

        return result;
    }

}
