package hitman;

import java.util.*;

public class Player {
	private String name;
	private boolean isAlive;
	private ArrayList<Card> hand;
	
	public Player(String name) {
		this.name = name;
		hand = new ArrayList<>();
		this.isAlive = true;
	}
	
	public String getName() { return this.name; }
	public boolean isAlive() { return this.isAlive; }
	
	public void addCard(Card c) {
		hand.add(c);
	}
	
	public void removeCard(Card c) {
		hand.remove(c);
	}
	
	public boolean hasAngel() {
		for(Card c : hand) {
			if(c.getName().equals("Angel")) return true;
		}
		return false;
	}
	
	public void removeAngel() {
	    for(int i = 0; i < hand.size(); i++) {
	        if(hand.get(i).getName().equals("Angel")) {
	            hand.remove(i);
	            return;
	        }
	    }
	}
	
	public void printHand() {
		for(Card c : hand) {
			System.out.print(c.getName() + " ");
		}
		System.out.println();
	}
	
	public ArrayList<Card> getHand(){
		return hand;
	}
	
	public void eliminate() {
		this.isAlive = false;
		System.out.println(this.name + " has been eliminated!");
	}
	
	public void playCard(Card c) throws InvalidPlayException {
		for(Card cr : hand) {
			if(cr.getName().equals(c.getName())) {
				hand.remove(cr);
				cr.play();
				return;
			}
		}
		
		throw new InvalidPlayException(this.name + " tried to play " + c.getName() + " but doesn't have it!");
	}
	
	private int extraTurns = 0;

	public void addExtraTurn() { extraTurns++; }
	public boolean hasExtraTurn() { return extraTurns > 0; }
	public void useExtraTurn() { extraTurns--; }
	public void sortHand() {
	    hand.sort((a, b) -> a.getName().compareTo(b.getName()));
	}
	public void clearExtraTurns() {
	    extraTurns = 0;
	}
}





