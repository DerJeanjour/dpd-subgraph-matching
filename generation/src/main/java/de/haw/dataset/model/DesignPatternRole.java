package de.haw.dataset.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DesignPatternRole {

    private String tag;

    private String location;

    private RoleClassType classType;


}
