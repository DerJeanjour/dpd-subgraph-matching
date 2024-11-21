package de.haw.dataset;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetType;
import de.haw.misc.utils.FileUtils;

import java.io.File;

public class DesignPatternLoader {

    private static final String BASE_PATH_JAVA = "datasets/java/";

    private static final String BASE_PATH_P_MART = BASE_PATH_JAVA + "p-mart/";

    private static final String BASE_PATH_PATTERN_EXAMPLE = BASE_PATH_JAVA + "test/patterns/";

    private static final String BASE_PATH_DPDf_EXAMPLE = BASE_PATH_JAVA + "dpdf/";

    public static File load( final DatasetType datasetType ) {
        return switch ( datasetType ) {
            case P_MART -> FileUtils.get( BASE_PATH_P_MART + "P-MARt.xml" );
            case PATTERN_EXAMPLES -> FileUtils.get( BASE_PATH_PATTERN_EXAMPLE + "patterns.csv" );
            case DPDf -> FileUtils.get( BASE_PATH_DPDf_EXAMPLE + "patterns.csv" );
            default -> throw new IllegalArgumentException(
                    "Dateset does not have a design pattern file: " + datasetType );
        };
    }

}
