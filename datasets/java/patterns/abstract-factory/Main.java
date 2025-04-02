// Abstract Product A
public interface AbstractProductA {
    String usefulFunctionA();
}

// Concrete Products for Product A
public class ConcreteProductA1 implements AbstractProductA {
    @Override
    public String usefulFunctionA() {
        return "The result of the product A1.";
    }
}

public class ConcreteProductA2 implements AbstractProductA {
    @Override
    public String usefulFunctionA() {
        return "The result of the product A2.";
    }
}

// Abstract Product B
public interface AbstractProductB {
    String usefulFunctionB();
    String anotherUsefulFunctionB(AbstractProductA collaborator);
}

// Concrete Products for Product B
public class ConcreteProductB1 implements AbstractProductB {
    @Override
    public String usefulFunctionB() {
        return "The result of the product B1.";
    }

    @Override
    public String anotherUsefulFunctionB(AbstractProductA collaborator) {
        String result = collaborator.usefulFunctionA();
        return "The result of the B1 collaborating with ( " + result + " )";
    }
}

public class ConcreteProductB2 implements AbstractProductB {
    @Override
    public String usefulFunctionB() {
        return "The result of the product B2.";
    }

    @Override
    public String anotherUsefulFunctionB(AbstractProductA collaborator) {
        String result = collaborator.usefulFunctionA();
        return "The result of the B2 collaborating with ( " + result + " )";
    }
}

// Abstract Factory
public interface AbstractFactory {
    AbstractProductA createProductA();
    AbstractProductB createProductB();
}

// Concrete Factories
public class ConcreteFactory1 implements AbstractFactory {
    @Override
    public AbstractProductA createProductA() {
        return new ConcreteProductA1();
    }

    @Override
    public AbstractProductB createProductB() {
        return new ConcreteProductB1();
    }
}

public class ConcreteFactory2 implements AbstractFactory {
    @Override
    public AbstractProductA createProductA() {
        return new ConcreteProductA2();
    }

    @Override
    public AbstractProductB createProductB() {
        return new ConcreteProductB2();
    }
}

// Client code that works with abstract factories and products.
public class Client {
    public static void clientCode(AbstractFactory factory) {
        AbstractProductA productA = factory.createProductA();
        AbstractProductB productB = factory.createProductB();

        System.out.println(productB.usefulFunctionB());
        System.out.println(productB.anotherUsefulFunctionB(productA));
    }

    public static void main(String[] args) {
        System.out.println("Client: Testing client code with the first factory type:");
        AbstractFactory factory1 = new ConcreteFactory1();
        clientCode(factory1);

        System.out.println("\nClient: Testing the same client code with the second factory type:");
        AbstractFactory factory2 = new ConcreteFactory2();
        clientCode(factory2);
    }
}