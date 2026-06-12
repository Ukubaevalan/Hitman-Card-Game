package hitman;

public class HitmanCard extends Card {
	
	public HitmanCard() {
		super("Hitman", "Draw this = eliminated.");
	}
	
	@Override
	public void play() {
		System.out.println("You drew a Hitman! You are eliminated.");
	}
}
