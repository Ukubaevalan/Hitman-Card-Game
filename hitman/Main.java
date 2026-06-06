package hitman;

public class Main {
    public static void main(String[] args) {
        new MyFrame();
    }
}

class PrintTask implements Runnable {
    private String name;
    private int count;

    public PrintTask(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public void run() {
        for(int i = 1; i <= count; i++) {
            System.out.println(name + " → " + i);
            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
                System.out.println(name + " interrupted!");
            }
        }
        System.out.println(name + " done!");
    }
}