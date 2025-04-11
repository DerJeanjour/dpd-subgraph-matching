package de.haw.dataset.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor( staticName = "of" )
public class Dataset {

    private final DatasetLanguage language;

    private final DatasetType type;

    private final String projectName;

    private String customPath;

    public String getName() {
        return this.type.name().toLowerCase() + "-" + this.projectName.toLowerCase()
                .replace( " ", "_" )
                .replace( ".", "" )
                .trim();
    }

    public boolean isCustom() {
        return DatasetType.CUSTOM.equals( this.type );
    }

}
