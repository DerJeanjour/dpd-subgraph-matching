package de.haw.example.abstractfactory.elf;


import de.haw.example.abstractfactory.Army;

public class ElfArmy implements Army {
    static final String DESCRIPTION = "This is the elven Army!";

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
