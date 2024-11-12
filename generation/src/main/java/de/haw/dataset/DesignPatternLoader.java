package de.haw.dataset;

import de.haw.misc.utils.FileUtils;

import java.io.File;

public class DesignPatternLoader {

    private static final String BASE_PATH_JAVA = "datasets/java/";

    private static final String BASE_PATH_P_MART = BASE_PATH_JAVA + "p-mart/";

    public static File load( final Dataset dataset ) {
        return switch ( dataset ) {
            case QUICK_UML, LEXI, J_REFACTORY, NETBEANS, J_UNIT, J_HOT_DRAW, MAPPER_XML, NUTCH, PMD ->
                    FileUtils.get( BASE_PATH_P_MART + "P-MARt.xml" );
            default -> throw new IllegalArgumentException( "Dateset does not have a design pattern file." );
        };
    }

}
