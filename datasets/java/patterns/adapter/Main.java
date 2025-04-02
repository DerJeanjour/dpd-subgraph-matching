public class AdapterPatternDemo {

    /**
     * The Target defines the domain-specific interface used by the client code.
     */
    static class Target {
        public String request() {
            return "Target: The default target's behavior.";
        }
    }

    /**
     * The Adaptee contains some useful behavior, but its interface is incompatible
     * with the existing client code. The Adaptee needs some adaptation before the
     * client code can use it.
     */
    static class Adaptee {
        public String specificRequest() {
            return ".eetpadA eht fo roivaheb laicepS";
        }
    }

    /**
     * The Adapter makes the Adaptee's interface compatible with the Target's interface.
     */
    static class Adapter extends Target {
        private Adaptee adaptee;

        public Adapter(Adaptee adaptee) {
            this.adaptee = adaptee;
        }

        @Override
        public String request() {
            String toReverse = adaptee.specificRequest();
            // Reverse the string using StringBuilder
            String reversed = new StringBuilder(toReverse).reverse().toString();
            return "Adapter: (TRANSLATED) " + reversed;
        }
    }

    /**
     * The client code supports all classes that follow the Target interface.
     */
    public static void clientCode(Target target) {
        System.out.println(target.request());
    }

    public static void main(String[] args) {
        System.out.println("Client: I can work just fine with the Target objects:");
        Target target = new Target();
        clientCode(target);

        System.out.println("\nClient: The Adaptee class has a weird interface. See, I don't understand it:");
        Adaptee adaptee = new Adaptee();
        System.out.println("Adaptee: " + adaptee.specificRequest());

        System.out.println("\nClient: But I can work with it via the Adapter:");
        Adapter adapter = new Adapter(adaptee);
        clientCode(adapter);
    }
}