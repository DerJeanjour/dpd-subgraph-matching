package de.haw.example.abstractfactory.elf;

import de.haw.example.abstractfactory.King;

public class ElfKing implements King {
    static final String DESCRIPTION = "This is the elven king!";

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
