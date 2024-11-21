package de.haw.example.abstractfactory.orc;

import de.haw.example.abstractfactory.Castle;

public class OrcCastle implements Castle {
    static final String DESCRIPTION = "This is the orc castle!";

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
