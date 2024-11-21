package de.haw.example.abstractfactory.elf;

import de.haw.example.abstractfactory.Castle;

public class ElfCastle implements Castle {
    static final String DESCRIPTION = "This is the elven castle!";

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
