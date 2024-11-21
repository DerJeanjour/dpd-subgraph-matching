package de.haw.example.abstractfactory.orc;


import de.haw.example.abstractfactory.Army;

public class OrcArmy implements Army {
    static final String DESCRIPTION = "This is the orc Army!";

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
