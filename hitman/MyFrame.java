package hitman;

import javax.swing.*;

public class MyFrame extends JFrame {
	
	// ---- Every Panel's main menu ----
    public MyFrame() {
        setTitle("Hitman Card Game");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(new MainMenuPanel(this)); 
        setVisible(true);
    }
    
    // ---- Change of Panels here ----
    public void showPanel(JPanel panel) {
        getContentPane().removeAll(); 
        getContentPane().add(panel);  
        revalidate();                 
        repaint();                 
    }
}