package hitman;

import java.util.*;

public class Deck {
	
	private ArrayList<Card> deck;
	
	public Deck() {
		deck = new ArrayList<>();
	}
	
	public Card draw() throws EmptyDeckException {
		if(deck.isEmpty()) {
			throw new EmptyDeckException("Cannot draw from an empty deck!");
		}
		return deck.remove(deck.size()-1);
	}
	
	public void addCard(Card c) {
		deck.add(c);
	}
	
	public void shuffle() {
	    Collections.shuffle(deck);
	}
	
	public int size() {
		return deck.size();
	}
	
	public void printDeck() {
		for(Card c : deck) {
			System.out.print(c.getName() + " ");
		}
		System.out.println();
	}
	
	public void insertAtRandom(Card c) {
	    Random random = new Random();
	    int position = random.nextInt(deck.size() + 1); 
	    deck.add(position, c); 
	}
	
	public void insertAt(int index, Card c) {
	    int actualIndex = deck.size() - index;
	    actualIndex = Math.max(0, Math.min(actualIndex, deck.size()));
	    deck.add(actualIndex, c);
	}
	
	public Card drawFromBottom() throws EmptyDeckException {
	    if(deck.isEmpty()) throw new EmptyDeckException("Deck is empty!");
	    return deck.remove(0); // bottom is index 0
	}

	public ArrayList<Card> getCards() {
	    return deck;
	}
}
