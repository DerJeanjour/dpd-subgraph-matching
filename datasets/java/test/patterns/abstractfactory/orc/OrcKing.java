package de.haw.example.abstractfactory.orc;

import de.haw.example.abstractfactory.King;

public class OrcKing implements King {
    static final String DESCRIPTION = "This is the orc king!";

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
