// The base Component interface defines operations that can be altered by decorators.
interface Component {
    String operation();
}

// Concrete Component providing the default implementation.
class ConcreteComponent implements Component {
    @Override
    public String operation() {
        return "ConcreteComponent";
    }
}

// The base Decorator class follows the same interface as other components.
// It holds a reference to a Component and delegates the operation to it.
abstract class Decorator implements Component {
    protected Component component;

    public Decorator(Component component) {
        this.component = component;
    }

    @Override
    public String operation() {
        return component.operation();
    }
}

// Concrete Decorator A alters the result of the wrapped component.
class ConcreteDecoratorA extends Decorator {
    public ConcreteDecoratorA(Component component) {
        super(component);
    }

    @Override
    public String operation() {
        return "ConcreteDecoratorA(" + super.operation() + ")";
    }
}

// Concrete Decorator B also alters the result of the wrapped component.
class ConcreteDecoratorB extends Decorator {
    public ConcreteDecoratorB(Component component) {
        super(component);
    }

    @Override
    public String operation() {
        return "ConcreteDecoratorB(" + super.operation() + ")";
    }
}

// Client code that works with all objects using the Component interface.
public class DecoratorPatternDemo {

    public static void clientCode(Component component) {
        System.out.println("RESULT: " + component.operation());
    }

    public static void main(String[] args) {
        // Simple component
        Component simple = new ConcreteComponent();
        System.out.println("Client: I've got a simple component:");
        clientCode(simple);
        System.out.println();

        // Decorated component: decorators can wrap either simple components or other decorators.
        Component decorator1 = new ConcreteDecoratorA(simple);
        Component decorator2 = new ConcreteDecoratorB(decorator1);
        System.out.println("Client: Now I've got a decorated component:");
        clientCode(decorator2);
    }
}