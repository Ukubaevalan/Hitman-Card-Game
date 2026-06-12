package hitman;

public abstract class Card {
    private String name;
    private String description;

    public Card(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return this.name; }
    public String getDescription() { return this.description; }

    public void printInfo() {
        System.out.printf("Name: %s\nDescription: %s\n", name, description);
    }

    public abstract void play();
}