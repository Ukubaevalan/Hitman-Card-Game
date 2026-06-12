package hitman;

public class AngelCard extends Card {
	
	public AngelCard() {
		super("Angel", "Blocks a drawn Hitman.");
	}
	
	@Override
	public void play() {
		System.out.println("Angel activated! Hitman defused.");
	}
}
