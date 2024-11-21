package de.haw.example.abstractfactory;

import de.haw.example.abstractfactory.elf.ElfKingdomFactory;
import de.haw.example.abstractfactory.orc.OrcKingdomFactory;

public class FactoryMaker {

    public enum KingdomType {
        ELF,
        ORC
    }

    public static KingdomFactory makeFactory( KingdomType type ) {
        return switch ( type ) {
            case ELF -> new ElfKingdomFactory();
            case ORC -> new OrcKingdomFactory();
        };
    }
}
