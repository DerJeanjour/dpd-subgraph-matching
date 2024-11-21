package de.haw.example.factorymethod;

import de.haw.example.factorymethod.weapon.AxeWeapon;
import de.haw.example.factorymethod.weapon.SpearWeapon;
import de.haw.example.factorymethod.weapon.Weapon;
import de.haw.example.factorymethod.weapon.WeaponType;

import java.util.Map;

public class ElfBlacksmith implements Blacksmith {

    final static Map<WeaponType, Weapon> ELFARSENAL = Map.of(
            WeaponType.AXE, new AxeWeapon(),
            WeaponType.SPEAR, new SpearWeapon()
    );

    public Weapon manufactureWeapon( WeaponType weaponType ) {
        return ELFARSENAL.get( weaponType );
    }
}
