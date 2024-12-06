package de.haw.dataset;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetType;
import de.haw.misc.utils.FileUtils;

import java.io.File;

public class DatasetLoader {

    private static final String BASE_PATH_SELF = "";

    private static final String BASE_PATH_JAVA = "datasets/java/";

    private static final String BASE_PATH_TEST = BASE_PATH_JAVA + "test/";

    private static final String BASE_PATH_PATTERN_EXAMPLES = BASE_PATH_TEST + "patterns/";

    private static final String BASE_PATH_P_MART = BASE_PATH_JAVA + "p-mart/";

    private static final String BASE_PATH_DPDf = BASE_PATH_JAVA + "dpdf/java_projects/";

    public static File load( final Dataset dataset ) {
        return FileUtils.get( getDirOfDataset( dataset.getType() ) + dataset.getProjectName() + "/" );
    }

    public static String getDirOfDataset( final DatasetType type ) {
        return switch ( type ) {
            case SELF -> BASE_PATH_SELF;
            case OWN -> BASE_PATH_TEST;
            case PATTERN_EXAMPLES -> BASE_PATH_PATTERN_EXAMPLES;
            case P_MART -> BASE_PATH_P_MART;
            case DPDf -> BASE_PATH_DPDf;
        };
    }

}
