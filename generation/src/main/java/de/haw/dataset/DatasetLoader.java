package de.haw.dataset;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetType;
import de.haw.misc.utils.FileUtils;

import java.io.File;

public class DatasetLoader {

    public static final String BASE_PATH_SELF = "";

    public static final String BASE_PATH_JAVA = "datasets/java/";

    public static final String BASE_PATH_CPP = "datasets/cpp/";

    public static final String BASE_PATH_PYTHON = "datasets/python/";

    public static final String BASE_PATH_TEST = BASE_PATH_JAVA + "test/";

    public static final String BASE_PATH_PATTERN_EXAMPLES = BASE_PATH_TEST + "patterns/";

    public static final String BASE_PATH_P_MART = BASE_PATH_JAVA + "p-mart/";

    public static final String BASE_PATH_DPDf = BASE_PATH_JAVA + "dpdf/java_projects/";

    public static final String BASE_PATH_CPP_PATTERNS = BASE_PATH_CPP + "patterns/";

    public static final String BASE_PATH_PYTHON_PATTERNS = BASE_PATH_PYTHON + "patterns/";

    public static final String BASE_PATH_JAVA_PATTERNS = BASE_PATH_JAVA + "patterns/";

    public static File load( final Dataset dataset ) {
        if ( dataset.isCustom() ) {
            return FileUtils.get( dataset.getCustomPath() );
        }
        return FileUtils.get( getDirOfDataset( dataset.getType() ) + dataset.getProjectName() + "/" );
    }

    public static String getDirOfDataset( final DatasetType type ) {
        return switch ( type ) {
            case SELF -> BASE_PATH_SELF;
            case OWN -> BASE_PATH_TEST;
            case PATTERN_EXAMPLES -> BASE_PATH_PATTERN_EXAMPLES;
            case P_MART -> BASE_PATH_P_MART;
            case DPDf -> BASE_PATH_DPDf;
            case CPP_PATTERNS -> BASE_PATH_CPP_PATTERNS;
            case PYTHON_PATTERNS -> BASE_PATH_PYTHON_PATTERNS;
            case JAVA_PATTERNS -> BASE_PATH_JAVA_PATTERNS;
            default -> throw new IllegalArgumentException( "Can't determine directory of type: " + type );
        };
    }

}
