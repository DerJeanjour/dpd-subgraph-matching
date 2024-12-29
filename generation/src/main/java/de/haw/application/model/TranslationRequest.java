package de.haw.application.model;

import de.haw.dataset.model.Dataset;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor( staticName = "of" )
public class TranslationRequest {

    private final Dataset dataset;

    private final int depth;

}
