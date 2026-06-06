package hitman;

public class ShuffleCard extends Card {
    public ShuffleCard() {
        super("Shuffle", "Fully randomize the deck.");
    }

    @Override
    public void play() {
        System.out.println("Deck has been shuffled!");
    }
}