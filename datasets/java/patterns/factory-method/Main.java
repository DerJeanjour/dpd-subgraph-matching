// Product interface declares the operations that all concrete products must implement.
public interface Product {
    String operation();
}

// Concrete Products provide various implementations of the Product interface.
class ConcreteProduct1 implements Product {
    @Override
    public String operation() {
        return "{Result of the ConcreteProduct1}";
    }
}

class ConcreteProduct2 implements Product {
    @Override
    public String operation() {
        return "{Result of the ConcreteProduct2}";
    }
}

// The Creator abstract class declares the factory method that returns a Product.
abstract class Creator {
    // The factory method to create Product objects.
    public abstract Product factoryMethod();

    // Some business logic that uses the product returned by the factory method.
    public String someOperation() {
        // Create a product using the factory method.
        Product product = factoryMethod();
        // Use the product.
        String result = "Creator: The same creator's code has just worked with " + product.operation();
        return result;
    }
}

// Concrete Creators override the factory method to change the resulting product's type.
class ConcreteCreator1 extends Creator {
    @Override
    public Product factoryMethod() {
        return new ConcreteProduct1();
    }
}

class ConcreteCreator2 extends Creator {
    @Override
    public Product factoryMethod() {
        return new ConcreteProduct2();
    }
}

// The client code works with a concrete creator through its base interface.
public class FactoryMethodDemo {
    public static void main(String[] args) {
        System.out.println("App: Launched with the ConcreteCreator1.");
        Creator creator1 = new ConcreteCreator1();
        clientCode(creator1);

        System.out.println();

        System.out.println("App: Launched with the ConcreteCreator2.");
        Creator creator2 = new ConcreteCreator2();
        clientCode(creator2);
    }

    public static void clientCode(Creator creator) {
        System.out.println("Client: I'm not aware of the creator's class, but it still works.");
        System.out.println(creator.someOperation());
    }
}