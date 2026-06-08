package hitman;

public class FutureCard extends Card {
    public FutureCard() {
        super("Future", "Peek at top 3 cards of the deck.");
    }

    @Override
    public void play() {
        System.out.println("You peek at the top 3 cards of the deck.");
    }
}