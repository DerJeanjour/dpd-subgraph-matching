package de.haw.example.factorymethod;

import de.haw.example.factorymethod.weapon.Weapon;
import de.haw.example.factorymethod.weapon.WeaponType;

public interface Blacksmith {
    Weapon manufactureWeapon( WeaponType weaponType );

}
