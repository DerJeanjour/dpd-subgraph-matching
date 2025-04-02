import java.util.ArrayList;
import java.util.List;

public class ObserverPatternDemo {

    // IObserver interface
    public interface IObserver {
        void update(String messageFromSubject);
    }

    // ISubject interface
    public interface ISubject {
        void attach(IObserver observer);
        void detach(IObserver observer);
        void notifyObservers();
    }

    // Subject class implementing ISubject
    public static class Subject implements ISubject {
        private List<IObserver> observers = new ArrayList<>();
        private String message;

        @Override
        public void attach(IObserver observer) {
            observers.add(observer);
        }

        @Override
        public void detach(IObserver observer) {
            observers.remove(observer);
        }

        @Override
        public void notifyObservers() {
            System.out.println("There are " + observers.size() + " observers in the list.");
            for (IObserver observer : observers) {
                observer.update(message);
            }
        }

        public void createMessage(String message) {
            this.message = message;
            notifyObservers();
        }

        public void someBusinessLogic() {
            this.message = "change message message";
            notifyObservers();
            System.out.println("I'm about to do something important");
        }
    }

    // Observer class implementing IObserver
    public static class Observer implements IObserver {
        private static int staticNumber = 0;
        private int number;
        private String messageFromSubject;
        private Subject subject;

        public Observer(Subject subject) {
            this.subject = subject;
            this.subject.attach(this);
            staticNumber++;
            number = staticNumber;
            System.out.println("Hi, I'm the Observer \"" + number + "\".");
        }

        @Override
        public void update(String messageFromSubject) {
            this.messageFromSubject = messageFromSubject;
            printInfo();
        }

        public void removeMeFromTheList() {
            subject.detach(this);
            System.out.println("Observer \"" + number + "\" removed from the list.");
        }

        public void printInfo() {
            System.out.println("Observer \"" + number + "\": a new message is available --> " + messageFromSubject);
        }
    }

    // Main method demonstrating the Observer Pattern in action
    public static void main(String[] args) {
        Subject subject = new Subject();
        Observer observer1 = new Observer(subject);
        Observer observer2 = new Observer(subject);
        Observer observer3 = new Observer(subject);
        Observer observer4;
        Observer observer5;

        subject.createMessage("Hello World! :D");
        observer3.removeMeFromTheList();

        subject.createMessage("The weather is hot today! :p");
        observer4 = new Observer(subject);

        observer2.removeMeFromTheList();
        observer5 = new Observer(subject);

        subject.createMessage("My new car is great! ;)");
        observer5.removeMeFromTheList();

        observer4.removeMeFromTheList();
        observer1.removeMeFromTheList();
    }
}