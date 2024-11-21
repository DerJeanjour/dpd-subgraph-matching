package de.haw.example.factorymethod;

import de.haw.example.factorymethod.weapon.Weapon;
import de.haw.example.factorymethod.weapon.WeaponType;

public class Main {

    public static void main( String[] args ) {

        Blacksmith blacksmith = new OrcBlacksmith();
        Weapon weapon = blacksmith.manufactureWeapon( WeaponType.SPEAR );
        System.out.println( blacksmith + "-" + weapon.getDescription() );
        weapon = blacksmith.manufactureWeapon( WeaponType.AXE );
        System.out.println( blacksmith + "-" + weapon.getDescription() );

        blacksmith = new ElfBlacksmith();
        weapon = blacksmith.manufactureWeapon( WeaponType.SPEAR );
        System.out.println( blacksmith + "-" + weapon.getDescription() );
        weapon = blacksmith.manufactureWeapon( WeaponType.AXE );
        System.out.println( blacksmith + "-" + weapon.getDescription() );
    }

}
