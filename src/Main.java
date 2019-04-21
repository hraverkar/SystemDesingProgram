public class Main {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        Elevator elevator = new Elevator();
        elevator.listeningThread = new Thread(elevator.new Listen());
        elevator.listeningThread.start();
        elevator.processingThread = new Thread(elevator.new Process());
        elevator.processingThread.start();
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
