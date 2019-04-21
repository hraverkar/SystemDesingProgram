import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

class Elevator {

    float location = 0;
    Direction direction = Direction.UP;
    State state = State.STOPPED;
    Door door = Door.CLOSED;
    Thread processingThread;
    Thread listeningThread;

    public class Request {
        public long time;
        Integer floor;
        Direction direction;

        public Request(long time, Integer floor, Direction direction) {
            this.time = time;
            this.floor = floor;
            this.direction = direction;
        }
    }

    public Comparator<Request> upComparator = Comparator.comparing(u -> u.floor);
    public Comparator<Request> downComparator = upComparator.reversed();
    Queue<Request> upQueue = new PriorityQueue<>(upComparator);
    Queue<Request> currentQueue = upQueue;
    Queue<Request> downQueue = new PriorityQueue<>(downComparator);

    public void call(int floor, Direction direction) {
        if (direction == Direction.UP) {
            if (floor >= location) {
                currentQueue.add(new Request(System.currentTimeMillis(), floor, direction));
            } else {
                upQueue.add(new Request(System.currentTimeMillis(), floor, direction));
            }
        } else {
            if (floor <= location) {
                currentQueue.add(new Request(System.currentTimeMillis(), floor, direction));
            } else {
                downQueue.add(new Request(System.currentTimeMillis(), floor, direction));
            }
        }
    }

    public void go(int floor) {
        call(floor, direction);
    }

    public void process() {
        while (true) {
            if (!upQueue.isEmpty() && !downQueue.isEmpty()) {
                Request r = currentQueue.poll();
                if (r != null) {
                    goToFloor(r.floor);
                } else {
                    preProcessNexrQueue();
                }
            }
        }
    }

    private void preProcessNexrQueue() {
        if (getLowestTimeUpQueue() > getLowestTimeDownQueue()) {
            this.direction = Direction.UP;
            currentQueue = upQueue;
            upQueue = new PriorityQueue<>(upComparator);
        } else {
            this.direction = Direction.DOWN;
            currentQueue = downQueue;
            downQueue = new PriorityQueue<>(downComparator);
        }
    }

    private long getLowestTimeDownQueue() {
        return Test(downQueue);
    }

    private long getLowestTimeUpQueue() {
        return Test(upQueue);
    }

    private long Test(Queue<Request> upQueue) {
        long lowest = Long.MAX_VALUE;
        for (Request r : upQueue) {
            if (r.time < lowest)
                lowest = r.time;
        }
        return lowest;
    }

    private void goToFloor(int floor) {
        state = state.MOVING;
        for (float i = location; i <= floor; i = (float) (i + 0.1)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        location = floor;
        door = Door.OPEN;
        state = state.STOPPED;
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        door = Door.CLOSED;
    }

    public class Process implements Runnable {
        @Override
        public void run() {
            process();
        }
    }

    public class Listen implements Runnable {
        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(9000);
                while (true) {
                    Socket socket = serverSocket.accept();
                    Thread thread = new Thread(new Worker(socket));
                    thread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public class Worker implements Runnable {
            private Socket s;

            public Worker(Socket socket) {
                this.s = socket;
            }

            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(s.getInputStream()));
                    String line;
                    while (true) {
                        if ((line = reader.readLine()) != null) {
                            String[] tokens = line.split(" ");
                            if (tokens.length == 3 && tokens[0].equals("call")) {
                                call(Integer.parseInt(tokens[1]), tokens[2].equals("up") ? Direction.UP : Direction.DOWN);
                            } else if (tokens.length == 2 && tokens[0].equals("go")) {
                                go(Integer.parseInt(tokens[1]));
                            } else {
                                s.getOutputStream().write("Wrong input".getBytes());
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}