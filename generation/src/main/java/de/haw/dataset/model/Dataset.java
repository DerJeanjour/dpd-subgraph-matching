package de.haw.dataset.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor( staticName = "of" )
public class Dataset {

    private final DatasetType type;

    private final String projectName;

    public String getName() {
        return this.type.name().toLowerCase() + "-" + this.projectName.toLowerCase()
                .replace( " ", "_" )
                .replace( ".", "" )
                .trim();
    }

}
