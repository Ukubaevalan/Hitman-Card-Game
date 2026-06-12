package hitman;

public class StopCard extends Card {
    public StopCard() {
        super("Stop", "Block an action card used against you.");
    }

    @Override
    public void play() {
        System.out.println("Action blocked!");
    }
}