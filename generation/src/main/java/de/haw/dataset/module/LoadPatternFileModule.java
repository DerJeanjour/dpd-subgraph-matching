package de.haw.dataset.module;

import de.haw.dataset.DesignPatternLoader;
import de.haw.dataset.model.Dataset;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class LoadPatternFileModule<Target> extends PipeModule<Dataset, File, Target> {

    @Override
    protected File processImpl( final Dataset dataset, final PipeContext ctx ) {
        File file = null;
        boolean exists = true;
        try {
            file = DesignPatternLoader.load( dataset );
        } catch ( IllegalArgumentException e ) {
            exists = false;
        }
        ctx.set( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, exists );
        return file;
    }
}
