package de.haw.misc.utils;

import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class CsvUtils {

    protected static final char DEFAULT_SEPARATOR = ',';

    /**
     * Custom csv mapping strategy to allow @CsvBindByName and @CsvBindByPosition annotation of csv model in parallel.
     * <a href="https://stackoverflow.com/a/58833974">Source</a>
     */
    private static class CsvMappingStrategy<T> extends ColumnPositionMappingStrategy<T> {

        @Override
        @SuppressWarnings( "rawtypes" )
        public String[] generateHeader( T bean ) throws CsvRequiredFieldEmptyException {
            final int numColumns = getFieldMap().values().size();
            super.generateHeader( bean );

            String[] header = new String[numColumns];

            BeanField beanField;
            for ( int i = 0; i < numColumns; i++ ) {
                beanField = findField( i );
                String columnHeaderName = extractHeaderName( beanField );
                header[i] = columnHeaderName;
            }
            return header;
        }

        @SuppressWarnings( "rawtypes" )
        private String extractHeaderName( final BeanField beanField ) {
            if ( beanField == null || beanField.getField() == null || beanField.getField()
                    .getDeclaredAnnotationsByType( CsvBindByName.class ).length == 0 ) {
                return StringUtils.EMPTY;
            }

            final CsvBindByName bindByNameAnnotation = beanField.getField()
                    .getDeclaredAnnotationsByType( CsvBindByName.class )[0];
            return bindByNameAnnotation.column();
        }

    }

    /**
     * Write a csv as byte array. The csv model class defines the column data.
     * <p>
     * Column name of the model field is defined with the @CsvBindByName annotation. Column position of the model field is annotated with the
     *
     * @param csvBeans     list of csv bean instances that are providing the data
     * @param csvBeanClass class of csv bean model (columns are annotated with @CsvBindByName and @CsvBindByPosition)
     * @return csv as byte array (UTF-8)
     */
    public static <T> byte[] write( List<T> csvBeans, Class<T> csvBeanClass ) {
        return write( csvBeans, csvBeanClass, DEFAULT_SEPARATOR );
    }

    /**
     * Write a csv as byte array. The csv model class defines the column data.
     * <p>
     * Column name of the model field is defined with the @CsvBindByName annotation. Column position of the model field is annotated with the
     *
     * @param csvBeans     list of csv bean instances that are providing the data
     * @param csvBeanClass class of csv bean model (columns are annotated with @CsvBindByName and @CsvBindByPosition)
     * @param separator    char used to separate the data
     * @return csv as byte array (UTF-8)
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public static <T> byte[] write( List<T> csvBeans, Class<T> csvBeanClass, char separator ) {
        final StringWriter writer = new StringWriter();
        String csv = "";
        try {
            final CsvMappingStrategy<T> mappingStrategy = new CsvMappingStrategy<>();
            mappingStrategy.setType( csvBeanClass );
            final StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder( writer ).withSeparator( separator )
                    .withMappingStrategy( mappingStrategy )
                    .build();
            beanToCsv.write( csvBeans );
            csv = writer.toString();

        } catch ( CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e ) {
            throw new RuntimeException( e );
        } finally {
            try {
                writer.close();
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
        return csv.getBytes( StandardCharsets.UTF_8 );
    }

    /**
     * Read a csv and retrieve the bean model data.
     *
     * @param csv          as byte array (UTF-8)
     * @param csvBeanClass class of csv bean model (columns are annotated with @CsvBindByName and @CsvBindByPosition)
     * @return list of bean model instances
     */
    public static <T> List<T> read( byte[] csv, Class<T> csvBeanClass ) {
        return read( csv, csvBeanClass, DEFAULT_SEPARATOR, false );
    }

    /**
     * Read a csv and retrieve the bean model data.
     *
     * @param csv           as byte array (UTF-8)
     * @param csvBeanClass  class of csv bean model (columns are annotated with @CsvBindByName and @CsvBindByPosition)
     * @param separator     char used to separate the data
     * @param skipFirstLine if <code>true</code>, skip first line
     * @return list of bean model instances
     */
    public static <T> List<T> read(
            final byte[] csv, final Class<T> csvBeanClass, final char separator, final boolean skipFirstLine ) {
        final StringReader reader = new StringReader( new String( csv, StandardCharsets.UTF_8 ) );
        // @formatter:off
        final CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>( reader )
                .withType( csvBeanClass )
                .withSeparator( separator )
                .withSkipLines( skipFirstLine ? 1 : 0 )
                .withFilter( lineEntries-> Arrays.stream( lineEntries ).anyMatch( StringUtils::isNotBlank ) ) // filter out blank lines
                .build();
        // @formatter:on
        return csvToBean.parse();
    }

}
