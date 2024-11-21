package de.haw.example.abstractfactory.elf;

import de.haw.example.abstractfactory.Army;
import de.haw.example.abstractfactory.Castle;
import de.haw.example.abstractfactory.King;
import de.haw.example.abstractfactory.KingdomFactory;

public class ElfKingdomFactory implements KingdomFactory {

    @Override
    public Castle createCastle() {
        return new ElfCastle();
    }

    @Override
    public King createKing() {
        return new ElfKing();
    }

    @Override
    public Army createArmy() {
        return new ElfArmy();
    }
}
