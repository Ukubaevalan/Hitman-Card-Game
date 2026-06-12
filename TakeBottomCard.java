package hitman;

public class TakeBottomCard extends Card {
    public TakeBottomCard() {
        super("Take Bottom", "Draw from the bottom of the deck instead.");
    }

    @Override
    public void play() {
        System.out.println("You draw from the bottom of the deck!");
    }
}