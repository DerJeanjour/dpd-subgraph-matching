package de.haw;

// Import statements

import java.util.ArrayList;
import java.util.List;

// Interface declaration
interface Animal {
    void makeSound();
}

// Base class
abstract class Mammal implements Animal {
    protected String name;

    public Mammal( String name ) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void eat();
}

// Derived class
class Dog extends Mammal {
    public Dog( String name ) {
        super( name );
    }

    @Override
    public void makeSound() {
        System.out.println( "Woof! Woof!" );
    }

    @Override
    public void eat() {
        System.out.println( name + " is eating dog food." );
    }

    public void fetch() {
        System.out.println( name + " is fetching the ball." );
    }
}

// Another derived class
class Cat extends Mammal {
    public Cat( String name ) {
        super( name );
    }

    @Override
    public void makeSound() {
        System.out.println( "Meow! Meow!" );
    }

    @Override
    public void eat() {
        System.out.println( name + " is eating cat food." );
    }

    public void scratch() {
        System.out.println( name + " is scratching the furniture." );
    }
}

public class Main {

    public static void main( String[] args ) {
        List<Mammal> animals = new ArrayList<>();

        // Creating objects
        Dog dog = new Dog( "Buddy" );
        Cat cat = new Cat( "Whiskers" );

        // Adding objects to the list
        animals.add( dog );
        animals.add( cat );

        // Iterating through the list
        for ( Mammal animal : animals ) {
            animal.makeSound();
            animal.eat();

            if ( animal instanceof Dog ) {
                ( ( Dog ) animal ).fetch();
            } else if ( animal instanceof Cat ) {
                ( ( Cat ) animal ).scratch();
            }
        }
    }

}