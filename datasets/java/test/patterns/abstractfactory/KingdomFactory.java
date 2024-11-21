package de.haw.example.abstractfactory;

public interface KingdomFactory {
    Castle createCastle();

    King createKing();

    Army createArmy();
}
