# Hitman Card Game

A two-player survival card game built in Java for the Java Programming Practice course at Sungkyunkwan University, Spring 2026. Inspired by Exploding Kittens, the game features a full Swing GUI, local two-player mode, and TCP/IP networked multiplayer with a multithreaded server.

## Overview

Players take turns drawing cards from a shared deck. Drawing a Hitman card eliminates the player — unless they hold an Angel card, in which case the Hitman is reinserted into the deck at a position of the player's choosing. The last player alive wins.

## Features

- 9 unique card types with distinct effects
- Local mode — two players share one screen and take turns
- Network mode — host or join a game over LAN using TCP/IP
- Multithreaded server handling both clients simultaneously
- Persistent leaderboard stored in CSV format
- Custom exceptions for invalid game states
- Colorful Swing UI with card-game-table aesthetics

## Card Types

| Card | Effect |
|------|--------|
| Hitman | Draw it = you are eliminated (unless you hold an Angel) |
| Angel | Cancels a drawn Hitman; the holder chooses where it goes back |
| Skip | Skip your turn without drawing |
| Future | Peek at the top 3 cards of the deck |
| Bomb | Force a target to take 2 turns (stackable with counter-Bomb) |
| Take Card | Steal a card — the target chooses which to give |
| Stop | Cancel an action card used against you |
| Shuffle | Fully randomize the deck |
| Take Bottom | Draw from the bottom of the deck instead of the top |

## How to Run

### Requirements
- Java JDK 11 or higher
- Eclipse IDE (recommended) or any Java IDE

### Running in Eclipse
1. Clone or download this repository
2. Open Eclipse → File → Import → General → Existing Projects into Workspace
3. Select the Hitman_Card_Game folder
4. Right-click Main.java → Run As → Java Application

### Running from the command line

cd Hitman_Card_Game/src
javac hitman/*.java
java hitman.Main


## How to Play (Network Mode)

Both players must be on the same WiFi network.

### Host:
1. Run the game and click "Network Game"
2. Enter your name
3. Click "Host Game" — the screen will show "Waiting for opponent..."
4. Share your local IP address with the other player

### Join:
1. Run the game and click "Network Game"
2. Enter your name and the host's IP address
3. Click "Join Game"

Once both players are connected, the game begins automatically.

## Project Structure


Hitman_Card_Game/
└── src/
    └── hitman/
        ├── Main.java                  # Entry point
        ├── MyFrame.java               # Main window
        ├── Card.java                  # Abstract base class
        ├── HitmanCard.java            # Card subclasses
        ├── AngelCard.java
        ├── SkipCard.java
        ├── FutureCard.java
        ├── BombCard.java
        ├── TakeCard.java
        ├── StopCard.java
        ├── ShuffleCard.java
        ├── TakeBottomCard.java
        ├── Deck.java                  # Deck operations
        ├── Player.java                # Player state and hand
        ├── Game.java                  # Core game logic
        ├── EmptyDeckException.java    # Custom exceptions
        ├── InvalidPlayException.java
        ├── FileManager.java           # Leaderboard CSV I/O
        ├── MainMenuPanel.java         # GUI panels
        ├── PlayerSetupPanel.java
        ├── GamePanel.java
        ├── GameOverPanel.java
        ├── LeaderboardPanel.java
        ├── NetworkMenuPanel.java
        ├── NetworkGamePanel.java
        ├── GameServer.java            # Networking
        ├── GameClient.java
        └── NetworkGameState.java


## Technical Highlights
- Object-Oriented Design — Abstract Card class with 9 polymorphic subclasses
- Encapsulation — All game state hidden behind public methods
- Exception Handling — Custom checked exceptions for game logic errors
- File I/O — Try-with-resources for safe leaderboard CSV reading/writing
- Multithreading — One reader thread per client on the server
- Plain-text protocol — Easy to debug network messages

## Team

| Member | Role |
|--------|------|
| Emil Ganbarli | Backend — game logic, file I/O |
| Alan Ukubaev | Frontend — Swing GUI panels, networking |
