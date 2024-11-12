package de.haw.dataset.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DesignPatterType {

    ABSTRACT_FACTORY( "Abstract Factory" ),
    BUILDER( "Builder" ),
    COMMAND( "Command" ),
    COMPOSITE( "Composite" ),
    OBSERVER( "Observer" ),
    SINGLETON( "Singleton" ),
    ADAPTER( "Adapter" ),
    FACTORY_METHOD( "Factory Method" ),
    STATE( "State" ),
    VISITOR( "Visitor" ),
    ITERATOR( "Iterator" ),
    DECORATOR( "Decorator" ),
    NULL_OBJECT( "Null Object" ),
    PROTOTYPE( "Prototype" ),
    STRATEGY( "Strategy" ),
    TEMPLATE_METHOD( "Template Method" ),
    FACADE( "Facade" ),
    BRIDGE( "Bridge" ),
    MEMENTO( "Memento" ),
    PROXY( "Proxy" );

    private final String name;
}
