package hitman;

import java.util.*;

public class Game {
	ArrayList<Player> players;
	Deck deck;
	int currentPlayerIndex;
	
	public Game(ArrayList<Player> players) {
	    this.players = players;
	    deck = new Deck();
	    this.currentPlayerIndex = 0;
	    setupDeck();
	    dealStartingHands();
	}
	
	public void setupDeck() {
		int n = players.size();
		int h = n - 1;
		int a = n - 1;
		int s = 3 * n;
		int f = 2 * n;
		int b = 3 * n;
		int t = n;
		int st = 2 * n;
		int sh = 2 * n;
		int tb = n;
		
		for(int i = 0; i < h; i++) {
			deck.addCard(new HitmanCard());
		}
		for(int i = 0; i < a; i++) {
			deck.addCard(new AngelCard());
		}
		for(int i = 0; i < s; i++) {
			deck.addCard(new SkipCard());
		}
		for(int i = 0; i < f; i++) {
			deck.addCard(new FutureCard());
		}
		for(int i = 0; i < b; i++) {
			deck.addCard(new BombCard());
		}
		for(int i = 0; i < t; i++) {
			deck.addCard(new TakeCard());
		}
		for(int i = 0; i < st; i++) {
			deck.addCard(new StopCard());
		}
		for(int i = 0; i < sh; i++) {
			deck.addCard(new ShuffleCard());
		}
		for(int i = 0; i < tb; i++) {
			deck.addCard(new TakeBottomCard());
		}
		
		deck.shuffle();
	}
	
	public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
	public int getDeckSize() { return deck.size(); }
	public Deck getDeck() { return deck; }
	
	public void nextTurn() {
	    boolean anyoneAlive = false;
	    for(Player p : players) {
	        if(p.isAlive()) { anyoneAlive = true; break; }
	    }
	    if(!anyoneAlive) return;

	    // check if current player has extra turns from Bomb
	    Player current = players.get(currentPlayerIndex);
	    if(current.hasExtraTurn()) {
	        current.useExtraTurn();
	        return; 
	    }

	    do {
	        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
	    } while(!players.get(currentPlayerIndex).isAlive());
	}
	
	public Player checkWinner() {
		int count = 0;
		Player pl = null;
		
		for(Player p : players) {
			if(p.isAlive()) {
				pl = p;
				count++;
			}
		}
		
		if(count > 1) {
			return null;
		}
		
		return pl;
	}
	
	public Card drawCard() throws EmptyDeckException {
	    Player p = players.get(currentPlayerIndex);
	    Card c = deck.draw();

	    if(c.getName().equals("Hitman") && !p.hasAngel()) {
	        p.eliminate();
	        p.getHand().clear();
	        return null;
	    }
	    else if(c.getName().equals("Hitman") && p.hasAngel()) {
	        p.removeAngel();
	        return c; 
	    }
	    else {
	        p.addCard(c);
	        return null;
	    }
	}
	
	private void dealStartingHands() {
	    for(Player p : players) {
	        p.addCard(new AngelCard()); // free Angel
	        
	        int dealt = 0;
	        while(dealt < 5) {
	            try {
	                Card c = deck.draw();
	                if(c.getName().equals("Hitman")) {
	                    deck.insertAtRandom(c); 
	                } else {
	                    p.addCard(c);
	                    dealt++;
	                }
	            } catch(EmptyDeckException e) {
	                break;
	            }
	        }
	    }
	}
	// Skip — current player skips their draw turn
	public void skipTurn() {
	    nextTurn();
	}

	// Shuffle — shuffle the deck
	public void shuffleDeck() {
	    deck.shuffle();
	}
	
	public void forceTurn(Player target) {
	    for(int i = 0; i < players.size(); i++) {
	        if(players.get(i) == target) {
	            currentPlayerIndex = i;
	            return;
	        }
	    }
	}

	// Future — peek at top 3 cards
	public ArrayList<Card> peekTop3() {
	    ArrayList<Card> peeked = new ArrayList<>();
	    ArrayList<Card> deckCards = deck.getCards();
	    int size = deckCards.size();
	    for(int i = size - 1; i >= Math.max(0, size - 3); i--) {
	        peeked.add(deckCards.get(i));
	    }
	    return peeked;
	}

	// Take Bottom — draw from bottom of deck
	public Card drawFromBottom() throws EmptyDeckException {
	    return deck.drawFromBottom();
	}

	// Bomb — target takes 2 turns
	public void bombPlayer(Player target, int turns) {
	    for(int i = 0; i < turns; i++) {
	        target.addExtraTurn();
	    }
	}

	// Take Card — steal random card from target
	public Card stealCard(Player target) {
	    if(target.getHand().isEmpty()) return null;
	    Random random = new Random();
	    int index = random.nextInt(target.getHand().size());
	    Card stolen = target.getHand().get(index);
	    target.removeCard(stolen);
	    return stolen;
	}

	public ArrayList<Player> getAlivePlayers() {
	    ArrayList<Player> alive = new ArrayList<>();
	    for(Player p : players) {
	        if(p.isAlive() && p != players.get(currentPlayerIndex)) {
	            alive.add(p);
	        }
	    }
	    return alive;
	}
}









