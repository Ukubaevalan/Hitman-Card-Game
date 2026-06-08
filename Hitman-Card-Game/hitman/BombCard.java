package hitman;

public class BombCard extends Card {
    public BombCard() {
        super("Bomb", "Force a target to take 2 turns. Stackable.");
    }

    @Override
    public void play() {
        System.out.println("Target is forced to take 2 turns!");
    }
}