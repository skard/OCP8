package stas.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/*Class to demonstrate how incrementing "normal" (i.e., thread unsafe) integers and incrementing
"atomic" (i.e., thread safe) integers are different: Incrementing a shared Integer object without locks can result
in a data race; however, incrementing a shared AtomicInteger will not result in a data race.*/

class AtomicVariable_DEMO {
    // Create two integer objects � one normal and another atomic � with same
    // initial value
    private static Integer integer = new Integer(0);
    private static AtomicInteger atomicInteger = new AtomicInteger(0);

    static class IntegerIncrementer extends Thread {
        public void run() {
            System.out.println("Incremented value of integer is: " + ++integer);
        }
    }

    static class AtomicIntegerIncrementer extends Thread {
        public void run() {
            System.out.println("Incremented value of atomic integer is: " + atomicInteger.incrementAndGet());
        }
    }

    public static void main(String[] args) {
        // create three threads each for incrementing atomic and "normal"
        // integers
        for (int i = 0; i < 20; i++) {
            new IntegerIncrementer().start();
            new AtomicIntegerIncrementer().start();
        }
    }
}
