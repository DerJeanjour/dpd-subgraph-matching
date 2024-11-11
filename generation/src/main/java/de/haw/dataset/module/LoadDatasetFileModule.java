package de.haw.dataset.module;

import de.haw.dataset.Dataset;
import de.haw.dataset.DatasetLoader;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
@NoArgsConstructor( staticName = "instance" )
public class LoadDatasetFileModule<Target> extends PipeModule<Dataset, File, Target> {

    @Override
    protected File processImpl( final Dataset dataset, final PipeContext ctx ) {
        return DatasetLoader.load( dataset );
    }
}
