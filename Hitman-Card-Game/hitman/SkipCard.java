package hitman;

public class SkipCard extends Card {
    public SkipCard() {
        super("Skip", "Skip your turn - no draw.");
    }

    @Override
    public void play() {
        System.out.println("Your turn is skipped.");
    }
}