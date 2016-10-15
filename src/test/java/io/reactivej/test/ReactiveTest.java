package io.reactivej.test;

import io.reactivej.*;
import org.junit.Test;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class ReactiveTest {

    public static class Greet implements Serializable {}

    public static class Greeting implements Serializable {
        
        public String message;
        public Greeting(String msg) {
            message = msg;
        }
    }

    public static class Greeter extends ReactiveComponent {

        private AbstractComponentBehavior defaultBehavior = new AbstractComponentBehavior(this) {
            @Override
            public void onMessage(Serializable msg) throws Exception {
                if (msg instanceof Greet) {
                    getSender().tell(new Greeting("hello reactive"), getSelf());
                }
            }
        };

        public Greeter() {
        }

        @Override
        public AbstractComponentBehavior getDefaultBehavior() {
            return defaultBehavior;
        }

        @Override
        public void onSupervise(SystemMessage msg) {

        }
    }

    public static class GreetPrinter extends ReactiveComponent {

        @Override
        public void onSupervise(SystemMessage msg) {

        }

        @Override
        public AbstractComponentBehavior getDefaultBehavior() {
            return defaultBehavior;
        }

        private AbstractComponentBehavior defaultBehavior = new AbstractComponentBehavior(this) {
            public void onMessage(Serializable msg) throws Exception {
                if (msg instanceof Greeting) {
                    Greeting greeting = (Greeting) msg;
                    System.out.println(greeting.message);
                }
            }
        };
    }

    public static class SupervisorWorkerProtocol {
        public static class RequestWork implements Serializable {}

        public static class Work implements Serializable {
            private int denominator;
            private int numerator;

            public Work(int d, int n) {
                this.denominator = d;
                this.numerator = n;
            }
        }

        public static class WorkResult implements Serializable {
            private double result;

            public WorkResult(double r) {
                this.result = r;
            }

            public double getResult() {
                return result;
            }
        }
    }

    public static class Supervisor extends ReactiveComponent {

        @Override
        public void onSupervise(SystemMessage msg) {
            if (msg instanceof Failure) {
                Failure failure = (Failure) msg;
                System.out.println("exception occured");
            }
        }

        @Override
        public AbstractComponentBehavior getDefaultBehavior() {
            return defaultBehavior;
        }

        private AbstractComponentBehavior defaultBehavior = new AbstractComponentBehavior(this) {
            public void onMessage(Serializable msg) throws Exception {
                if (msg instanceof SupervisorWorkerProtocol.RequestWork) {
                    SupervisorWorkerProtocol.Work w = new SupervisorWorkerProtocol.Work(0, 4);
                    getSender().tell(w, getSelf());
                } else if (msg instanceof SupervisorWorkerProtocol.WorkResult) {
                    SupervisorWorkerProtocol.WorkResult r = (SupervisorWorkerProtocol.WorkResult) msg;
                    System.out.println("result: " + r.getResult());
                }
            }
        };
    }

    public static class Worker extends ReactiveComponent {

        @Override
        public void onSupervise(SystemMessage msg) {

        }

        @Override
        public AbstractComponentBehavior getDefaultBehavior() {
            return defaultBehavior;
        }

        private AbstractComponentBehavior defaultBehavior = new AbstractComponentBehavior(this) {
            public void onMessage(Serializable msg) throws Exception {
                if (msg instanceof SupervisorWorkerProtocol.Work) {
                    SupervisorWorkerProtocol.Work work = (SupervisorWorkerProtocol.Work) msg;
                    double result = work.numerator / work.denominator;
                    getSender().tell(new SupervisorWorkerProtocol.WorkResult(result), getSelf());
                }
            }
        };
    }

    @Test
    public void localTest() throws InterruptedException {
        ReactiveSystem sys1 = new ReactiveSystem(1991, "127.0.0.1:2181", "test_reactive");
        sys1.init();
        ReactiveRef greeter = sys1.createReactiveComponent("greeter", true, Greeter.class.getName());
        ReactiveRef greetPrinter = sys1.createReactiveComponent("greetPrinter", true, GreetPrinter.class.getName());
        sys1.getScheduler().scheduleOnce(0, greeter, new Greet(), greetPrinter);

        Thread.currentThread().join();
    }

    @Test
    public void remoteTest() throws InterruptedException {
        ReactiveSystem sys1 = new ReactiveSystem(1991, "127.0.0.1:2181", "test_reactive");
        sys1.init();
        ReactiveSystem sys2 = new ReactiveSystem(1992, "127.0.0.1:2181", "test_reactive");
        sys2.init();
        ReactiveRef greeter = sys1.createReactiveComponent("greeter", true, Greeter.class.getName());
        ReactiveRef greetPrinter = sys2.createReactiveComponent("greetPrinter", true, GreetPrinter.class.getName());
        sys2.getScheduler().scheduleOnce(0, greeter, new Greet(), greetPrinter);

        Thread.currentThread().join();
    }

    @Test
    public void superviseTest() throws InterruptedException {
        ReactiveSystem sys1 = new ReactiveSystem(1991, "127.0.0.1:2181", "test_reactive");
        sys1.init();
        ReactiveRef supervisor = sys1.createReactiveComponent("supervisor", true, Supervisor.class.getName());
        ReactiveRef worker = supervisor.createReactiveComponent("worker", true, Worker.class.getName());
        sys1.getScheduler().scheduleOnce(0, supervisor, new SupervisorWorkerProtocol.RequestWork(), worker);

        Thread.currentThread().join();
    }
}
