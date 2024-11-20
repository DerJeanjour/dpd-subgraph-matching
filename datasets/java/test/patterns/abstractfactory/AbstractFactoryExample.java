package de.haw.test;

interface Shape {
    void draw();
}

class Circle implements Shape {
    @Override
    public void draw() {
        System.out.println("Drawing a Circle");
    }
}

class Rectangle implements Shape {
    @Override
    public void draw() {
        System.out.println("Drawing a Rectangle");
    }
}

abstract class AbstractFactory {
    abstract Shape getShape(String shapeType);
}

class RoundedShapeFactory extends AbstractFactory {

    @Override
    Shape getShape(String shapeType) {
        if (shapeType.equalsIgnoreCase("CIRCLE")) {
            return new Circle(); // Assuming all shapes are "rounded"
        } else if (shapeType.equalsIgnoreCase("RECTANGLE")) {
            return new Rectangle();
        }
        return null;
    }

}

class RegularShapeFactory extends AbstractFactory {
    @Override
    Shape getShape(String shapeType) {
        if (shapeType.equalsIgnoreCase("CIRCLE")) {
            return new Circle();
        } else if (shapeType.equalsIgnoreCase("RECTANGLE")) {
            return new Rectangle();
        }
        return null;
    }
}

class FactoryProducer {
    public static AbstractFactory getFactory(boolean rounded) {
        if (rounded) {
            return new RoundedShapeFactory();
        } else {
            return new RegularShapeFactory();
        }
    }
}

public class AbstractFactoryExample {
    public static void main(String[] args) {

        AbstractFactory shapeFactory = FactoryProducer.getFactory(true);

        Shape shape1 = shapeFactory.getShape("CIRCLE");
        shape1.draw();

        Shape shape2 = shapeFactory.getShape("RECTANGLE");
        shape2.draw();

        AbstractFactory regularFactory = FactoryProducer.getFactory(false);

        Shape shape3 = regularFactory.getShape("CIRCLE");
        shape3.draw();

        Shape shape4 = regularFactory.getShape("RECTANGLE");
        shape4.draw();
    }
}