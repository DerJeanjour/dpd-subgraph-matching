package de.haw.dataset.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum DesignPatternType {

    /* --- PRIMARY --- */
    // https://java-design-patterns.com/patterns/abstract-factory/
    ABSTRACT_FACTORY( "Abstract Factory" ),
    // https://java-design-patterns.com/patterns/adapter/
    ADAPTER( "Adapter" ),
    // https://java-design-patterns.com/patterns/builder/
    BUILDER( "Builder" ),
    // https://java-design-patterns.com/patterns/facade/
    FACADE( "Facade" ),
    // https://java-design-patterns.com/patterns/factory-method/
    FACTORY_METHOD( "Factory Method" ),
    // https://java-design-patterns.com/patterns/observer/
    OBSERVER( "Observer" ),
    // https://java-design-patterns.com/patterns/singleton/
    SINGLETON( "Singleton" ),

    /* --- SECONDARY --- */
    DECORATOR( "Decorator" ),
    MEMENTO( "Memento" ),
    PROTOTYPE( "Prototype" ),
    PROXY( "Proxy" ),
    VISITOR( "Visitor" ),

    /* --- ADDITIONAL --- */
    COMMAND( "Command" ),
    COMPOSITE( "Composite" ),
    STATE( "State" ),
    ITERATOR( "Iterator" ),
    NULL_OBJECT( "Null Object" ),
    TEMPLATE_METHOD( "Template Method" ),
    BRIDGE( "Bridge" ),
    STRATEGY( "Strategy" );

    private final String name;

    public static List<String> getLabels() {
        return Arrays.stream( DesignPatternType.values() ).map( DesignPatternType::name ).toList();
    }

}
