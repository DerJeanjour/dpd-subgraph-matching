package de.haw.example.abstractfactory.orc;

import de.haw.example.abstractfactory.Army;
import de.haw.example.abstractfactory.Castle;
import de.haw.example.abstractfactory.King;
import de.haw.example.abstractfactory.KingdomFactory;

public class OrcKingdomFactory implements KingdomFactory {

    @Override
    public Castle createCastle() {
        return new OrcCastle();
    }

    @Override
    public King createKing() {
        return new OrcKing();
    }

    @Override
    public Army createArmy() {
        return new OrcArmy();
    }
}
