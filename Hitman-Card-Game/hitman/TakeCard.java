package hitman;

public class TakeCard extends Card {
    public TakeCard() {
        super("Take Card", "Steal from target — they pick what to give.");
    }

    @Override
    public void play() {
        System.out.println("You steal a card from target!");
    }
}