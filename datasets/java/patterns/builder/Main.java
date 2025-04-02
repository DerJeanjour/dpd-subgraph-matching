import java.util.ArrayList;
import java.util.List;

// The Product class represents the complex object under construction.
class Product {
    private List<String> parts = new ArrayList<>();

    public void addPart(String part) {
        parts.add(part);
    }

    public void listParts() {
        System.out.print("Product parts: ");
        for (int i = 0; i < parts.size(); i++) {
            System.out.print(parts.get(i));
            if (i != parts.size() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("\n");
    }
}

// The Builder interface declares product construction steps.
interface Builder {
    void producePartA();
    void producePartB();
    void producePartC();
}

// ConcreteBuilder1 provides specific implementations for the building steps.
class ConcreteBuilder1 implements Builder {
    private Product product;

    public ConcreteBuilder1() {
        reset();
    }

    // Resets the builder to start a fresh product.
    private void reset() {
        product = new Product();
    }

    @Override
    public void producePartA() {
        product.addPart("PartA1");
    }

    @Override
    public void producePartB() {
        product.addPart("PartB1");
    }

    @Override
    public void producePartC() {
        product.addPart("PartC1");
    }

    // Returns the product and resets the builder for future use.
    public Product getProduct() {
        Product result = product;
        reset();
        return result;
    }
}

// The Director class defines the order in which to call construction steps.
class Director {
    private Builder builder;

    public void setBuilder(Builder builder) {
        this.builder = builder;
    }

    // Constructs a minimal viable product (only PartA).
    public void buildMinimalViableProduct() {
        builder.producePartA();
    }

    // Constructs a full featured product (Parts A, B, and C).
    public void buildFullFeaturedProduct() {
        builder.producePartA();
        builder.producePartB();
        builder.producePartC();
    }
}

// Client code to demonstrate the Builder pattern usage.
public class BuilderPatternExample {
    public static void clientCode(Director director) {
        ConcreteBuilder1 builder = new ConcreteBuilder1();
        director.setBuilder(builder);

        System.out.println("Standard basic product:");
        director.buildMinimalViableProduct();
        Product product = builder.getProduct();
        product.listParts();

        System.out.println("Standard full featured product:");
        director.buildFullFeaturedProduct();
        product = builder.getProduct();
        product.listParts();

        // The Builder pattern can also be used without a Director.
        System.out.println("Custom product:");
        builder.producePartA();
        builder.producePartC();
        product = builder.getProduct();
        product.listParts();
    }

    public static void main(String[] args) {
        Director director = new Director();
        clientCode(director);
    }
}