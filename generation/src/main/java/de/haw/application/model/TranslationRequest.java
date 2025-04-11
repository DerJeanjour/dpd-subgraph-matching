package de.haw.application.model;

import de.haw.dataset.model.Dataset;
import de.haw.dataset.model.DatasetLanguage;
import de.haw.dataset.model.DatasetType;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class TranslationRequest {

    private final Dataset dataset;

    private final int depth;

    public static TranslationRequest of( final Dataset dataset, final int depth ) {
        return new TranslationRequest( dataset, depth );
    }

    public static TranslationRequest custom(
            final String customPath, final String projectName, final DatasetLanguage language, final int depth ) {
        if ( StringUtils.isBlank( customPath ) ) {
            throw new IllegalArgumentException( "Can't process path: " + customPath );
        }
        final Dataset dataset = Dataset.of( language, DatasetType.CUSTOM, projectName );
        dataset.setCustomPath( customPath );
        return new TranslationRequest( dataset, depth );
    }

}
