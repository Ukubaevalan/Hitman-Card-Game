package hitman;

import java.io.*;
import java.util.*;

public class FileManager {

    public FileManager() {}

    // save or update a winner's win count
    public void saveWinner(String name) {
        ArrayList<String> entries = loadLeaderboard();
        boolean found = false;

        for(int i = 0; i < entries.size(); i++) {
            String[] parts = entries.get(i).split(",");
            if(parts[0].equals(name)) {
                int wins = Integer.parseInt(parts[1]) + 1;
                entries.set(i, name + "," + wins);
                found = true;
                break;
            }
        }

        if(!found) {
            entries.add(name + ",1");
        }

        saveLeaderboard(entries);
    }

    public void saveLeaderboard(ArrayList<String> entries) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("leaderboard.csv"))) {
            for(String entry : entries) {
                bw.write(entry);
                bw.newLine();
            }
        } catch(IOException e) {
            System.out.println("Error saving: " + e.getMessage());
        }
    }

    public ArrayList<String> loadLeaderboard() {
        ArrayList<String> result = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader("leaderboard.csv"))) {
            String line;
            while((line = br.readLine()) != null) {
                result.add(line);
            }
        } catch(IOException e) {
            System.out.println("No leaderboard found, starting fresh.");
        }
        return result;
    }
}