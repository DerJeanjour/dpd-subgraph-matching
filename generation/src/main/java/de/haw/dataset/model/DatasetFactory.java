package de.haw.dataset.model;

import de.haw.dataset.DatasetLoader;
import de.haw.misc.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class DatasetFactory {

    public static final Dataset SELF = get( DatasetProject.SELF );
    public static final Dataset ANIMAL = get( DatasetProject.ANIMAL );
    public static final Dataset CHESS = get( DatasetProject.CHESS );
    public static final Dataset ABSTRACT_FACTORY_EXAMPLE = get( DatasetProject.ABSTRACT_FACTORY_EXAMPLE );
    public static final Dataset ADAPTER_EXAMPLE = get( DatasetProject.ADAPTER_EXAMPLE );
    public static final Dataset BUILDER_EXAMPLE = get( DatasetProject.BUILDER_EXAMPLE );
    public static final Dataset FACADE_EXAMPLE = get( DatasetProject.FACADE_EXAMPLE );
    public static final Dataset FACTORY_METHOD_EXAMPLE = get( DatasetProject.FACTORY_METHOD_EXAMPLE );
    public static final Dataset OBSERVER_EXAMPLE = get( DatasetProject.OBSERVER_EXAMPLE );
    public static final Dataset SINGLETON_EXAMPLE = get( DatasetProject.SINGLETON_EXAMPLE );
    public static final Dataset QUICK_UML = get( DatasetProject.QUICK_UML );
    public static final Dataset LEXI = get( DatasetProject.LEXI );
    public static final Dataset J_REFACTORY = get( DatasetProject.J_REFACTORY );
    public static final Dataset NETBEANS = get( DatasetProject.NETBEANS );
    public static final Dataset J_UNIT = get( DatasetProject.J_UNIT );
    public static final Dataset J_HOT_DRAW = get( DatasetProject.J_HOT_DRAW );
    public static final Dataset MAPPER_XML = get( DatasetProject.MAPPER_XML );
    public static final Dataset NUTCH = get( DatasetProject.NUTCH );
    public static final Dataset PMD = get( DatasetProject.PMD );

    public static Dataset get( final DatasetProject project ) {

        final String projectName = switch ( project ) {
            case SELF -> "generation";
            case ANIMAL -> "animals";
            case CHESS -> "chess";
            case ABSTRACT_FACTORY_EXAMPLE -> "abstractfactory";
            case ADAPTER_EXAMPLE -> "adapter";
            case BUILDER_EXAMPLE -> "builder";
            case FACADE_EXAMPLE -> "facade";
            case FACTORY_METHOD_EXAMPLE -> "factorymethod";
            case OBSERVER_EXAMPLE -> "observer";
            case SINGLETON_EXAMPLE -> "singleton";
            case QUICK_UML -> "1 - QuickUML 2001";
            case LEXI -> "2 - Lexi v0.1.1 alpha";
            case J_REFACTORY -> "3 - JRefactory v2.6.24";
            case NETBEANS -> "4 - Netbeans v1.0.x";
            case J_UNIT -> "5 - JUnit v3.7";
            case J_HOT_DRAW -> "6 - JHotDraw v5.1";
            case MAPPER_XML -> "8 - MapperXML v1.9.7";
            case NUTCH -> "10 - Nutch v0.4";
            case PMD -> "11 - PMD v1.8";
        };

        return get( project.getType(), projectName );
    }

    public static DatasetLanguage getLanguage( final DatasetType type ) {
        return switch ( type ) {
            case P_MART, SELF, DPDf, OWN, PATTERN_EXAMPLES, JAVA_PATTERNS -> DatasetLanguage.JAVA;
            case CPP_PATTERNS -> DatasetLanguage.CPP;
            case PYTHON_PATTERNS -> DatasetLanguage.PYTHON;
        };
    }

    public static Dataset get( final DatasetType type, final String projectName ) {
        if ( StringUtils.isBlank( projectName ) ) {
            throw new IllegalArgumentException( "Can't create dataset with empty project name!" );
        }
        return Dataset.of( getLanguage( type ), type, projectName );
    }

    public static List<Dataset> getAll( final DatasetType type ) {
        return FileUtils.getDirectoryNamesInDir( DatasetLoader.getDirOfDataset( type ) )
                .stream()
                .map( projectName -> Dataset.of( getLanguage( type ), type, projectName ) )
                .toList();
    }

}
