package de.haw.dataset;

import de.haw.misc.utils.FileUtils;

import java.io.File;

public class DatasetLoader {

    private static final String BASE_PATH_JAVA = "datasets/java/";

    private static final String BASE_PATH_TEST = BASE_PATH_JAVA + "test/";

    private static final String BASE_PATH_P_MART = BASE_PATH_JAVA + "p-mart/";

    public static File load( final Dataset dataset ) {
        return switch ( dataset ) {
            case SELF -> load( "generation/src/main/java/de/haw/" );
            case ANIMAL -> load( BASE_PATH_TEST + "animals/" );
            case CHESS -> load( BASE_PATH_TEST + "chess/" );
            case QUICK_UML -> load( BASE_PATH_P_MART + "1 - QuickUML 2001/" );
            case LEXI -> load( BASE_PATH_P_MART + "2 - Lexi v0.1.1 alpha/" );
            case J_REFACTORY -> load( BASE_PATH_P_MART + "3 - JRefactory v2.6.24/" );
            case NETBEANS -> load( BASE_PATH_P_MART + "4 - Netbeans v1.0.x/" );
            case J_UNIT -> load( BASE_PATH_P_MART + "5 - JUnit v3.7/" );
            case J_HOT_DRAW -> load( BASE_PATH_P_MART + "6 - JHotDraw v5.1/" );
            case MAPPER_XML -> load( BASE_PATH_P_MART + "8 - MapperXML v1.9.7/" );
            case NUTCH -> load( BASE_PATH_P_MART + "10 - Nutch v0.4/" );
            case PMD -> load( BASE_PATH_P_MART + "11 - PMD v1.8/" );
        };
    }

    private static File load( final String path ) {
        return FileUtils.get( path );
    }

}
