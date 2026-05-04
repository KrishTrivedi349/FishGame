import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class FishGame extends JFrame implements KeyListener {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    private static final int DOCK_Y = 85;
    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 50;
    private static final int OBSTACLE_WIDTH = 20;
    private static final int OBSTACLE_HEIGHT = 20;
    private static final int PLAYER_SPEED = 25;

    private int currentObstacleSpeed = 3;
    private double fishSpawnRate = 0.03;
    private double puffSpawnRate = 0.005;
    private int level = 1;
    private Timer timer;
    private int timeLeft = 60;
    private static final int MAX_LEVEL = 5;
    private boolean showLevelUp = false;
    private long levelUpStartTime = 0;
    private static final int LEVEL_UP_DISPLAY_MSG = 2000;


    private int score = 0;
    private int health = 100;
    private int lives = 3;



    private JPanel gamePanel;
    private JLabel scoreLabel;
    private JLabel healthLabel;
    private JLabel timerLabel;


    private boolean isGameOver;


    private int playerX, playerY;
    private int poleY;
    private int poleSpeed;
    private boolean poleDeployed;

    private final int poleStartY = 90;
    private final int poleEndY = HEIGHT - 20;

    private List<GameObject> objects = new ArrayList<>();
    private List<Point> stars = new ArrayList<>();

    private BufferedImage yodaImage, exploreImage, alphaImage;
    private BufferedImage currentCatImage;

    private boolean shieldActive = false;
    private long shieldStartTime;
    private long lastShieldUseTime;
    private static final int SHIELD_DURATION = 3000;
    private static final int SHIELD_COOLDOWN = 30000;

    private long lastSecondTick = System.currentTimeMillis();

    private void applyLevelDifficulty(){
        // Speed grows by 2 each level: 3, 5, 7, 9, 11
        currentObstacleSpeed = 3 + (level - 1) * 2;

        // Fish spawn rate grows by 0.015 each level: 0.03, 0.045, 0.06 …
        fishSpawnRate = 0.03 + (level - 1) * 0.015;

        // Pufferfish rate grows by 0.004 each level: 0.005, 0.009, 0.013 …
        puffSpawnRate = 0.005 + (level - 1) * 0.004;

    }


    private List<Point> generateStars(int numStars) {
        List<Point> starsList = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numStars; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            starsList.add(new Point(x, y));
        }
        return starsList;
    }

    private void activateShield() {
        long now = System.currentTimeMillis();
        if (!shieldActive && now - lastShieldUseTime > SHIELD_COOLDOWN) {
            shieldActive = true;
            shieldStartTime = now;
            lastShieldUseTime = now;
        }
    }

    private boolean isShieldActive() {
        return shieldActive && (System.currentTimeMillis() - shieldStartTime) < SHIELD_DURATION;
    }

    private void reset() {
        score = 0;
        health = 100;
        lives = 3;
        level = 1;
        timeLeft = 60;
        showLevelUp = false;
        isGameOver = false;
        shieldActive = false;
        poleDeployed = false;
        poleY = poleStartY;
        objects.clear();
        applyLevelDifficulty();
        repaint();
    }

    class GameObject{
        int x, y;
        String type;

        public GameObject(int x,int y, String type){
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    public FishGame() {
        try {
            yodaImage = ImageIO.read(new File("C:/Users/zi042/OneDrive/Pictures/yoda.png"));
            exploreImage = ImageIO.read(new File("C:/Users/zi042/OneDrive/Pictures/explore.png"));
            alphaImage = ImageIO.read(new File("C:/Users/zi042/OneDrive/Pictures/alpha.png"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        String[] options = {"Yoda", "Explore", "Alpha"};
        int choice = JOptionPane.showOptionDialog(
                null, "Choose your cat:", "Cat Selection",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            currentCatImage = yodaImage;
            poleSpeed = 3;
        } else if (choice == 1) {
            currentCatImage = exploreImage;
            poleSpeed = 6;
        } else {
            currentCatImage = alphaImage;
            poleSpeed = 9;
        }

        setTitle("Underwater Cat Fishing");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };

        scoreLabel = new JLabel("Score: 0 | Level: 1");
        scoreLabel.setForeground(Color.BLUE);
        gamePanel.add(scoreLabel);

        healthLabel = new JLabel("Lives: 3 | Health: 100");
        healthLabel.setForeground(Color.RED);
        gamePanel.add(healthLabel);

        timerLabel = new JLabel("Time: 60");
        timerLabel.setForeground(Color.YELLOW);
        gamePanel.add(timerLabel);

        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = 40;
        poleY = poleStartY;
        poleDeployed = false;

        isGameOver = false;
        objects = new ArrayList<>();

        applyLevelDifficulty();

        timer = new Timer(20, e -> {
            if (!isGameOver) {
                update();
                repaint();
            }
        });
        timer.start();
    }

    public static Color generateRandomColor() {
        Random rand = new Random();
        int r = rand.nextInt(256);
        int g = rand.nextInt(256);
        int b = rand.nextInt(256);
        return new Color(r, g, b);
    }

    private void draw(Graphics g) {
        g.setColor(new Color(135,206,235));
        g.fillRect(0, 0, WIDTH, DOCK_Y);

        g.setColor(new Color(0, 100, 150));
        g.fillRect(0, DOCK_Y, WIDTH, HEIGHT - DOCK_Y);

        g.setColor(Color.CYAN);
        for (Point star : stars) {
            g.fillOval(star.x, star.y, 2, 2);
        }

        g.setColor(new Color(139,69,19));
        g.fillRect(0, DOCK_Y, WIDTH, 15);

        if (currentCatImage != null) {
            g.drawImage(currentCatImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null);
        } else {
            g.setColor(Color.BLACK);
            g.fillOval(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
            g.setColor(Color.WHITE);
            g.fillOval(playerX + 12, playerY + 12, 10, 10);
            g.fillOval(playerX + 28, playerY + 12, 10, 10);
        }

        g.setColor(Color.BLACK);
        g.drawLine(playerX + PLAYER_WIDTH / 2, playerY + PLAYER_HEIGHT, playerX + PLAYER_WIDTH / 2, poleY);
        g.setColor(Color.WHITE);
        g.fillOval(playerX + PLAYER_WIDTH / 2 - 3, poleY - 3, 6, 6);

        g.setColor(Color.BLACK);
        for(GameObject object : objects){
            if(object.type.equals("pufferfish")){
                g.setColor(Color.MAGENTA); // stands out
            } else if(object.type.equals("powerup")){
                g.setColor(Color.GREEN);
            } else {
                g.setColor(Color.ORANGE); // normal fish
            }

            g.fillOval(object.x, object.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
        }

        for(int i = 0; i < lives; i++){
            g.setColor(Color.PINK);
            g.fillOval(10 + (i * 30),50,15,15);
        }

        g.setColor(Color.GRAY);
        g.fillRect(10,30,100,10);

        g.setColor(Color.GREEN);
        g.fillRect(10,30,health,10);

        g.setColor(Color.BLACK);
        g.fillRect(10,30,100,10);

        if(health > 60){
            g.setColor(Color.GREEN);
        }else if(health > 30){
            g.setColor(Color.YELLOW);
        }else{
            g.setColor(Color.RED);
        }
        g.fillRect(10,30,health,10);


        if (isShieldActive()) {
            g.setColor(new Color(0, 255, 255, 120));
            g.fillOval(playerX - 5, playerY - 5, PLAYER_WIDTH + 10, PLAYER_HEIGHT + 10);
        }


        if (showLevelUp) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(WIDTH / 2 - 110, HEIGHT / 2 - 40, 220, 70, 20, 20);
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("LEVEL UP!", WIDTH / 2 - 70, HEIGHT / 2 - 10);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Level " + level + " — Good luck!", WIDTH / 2 - 72, HEIGHT / 2 + 20);
        }

        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Game Over!", WIDTH / 2 - 80, HEIGHT / 2);
        }
    }

    private void update() {

        if (showLevelUp &&
                System.currentTimeMillis() - levelUpStartTime > LEVEL_UP_DISPLAY_MSG) {
            showLevelUp = false;
        }

        if(poleDeployed){
            if(poleY < poleEndY){
                poleY += poleSpeed;
            }
        }

        if (!poleDeployed) {
            if(poleY > poleStartY){
                poleY -= poleSpeed;
            }
        }

        if(Math.random() < 0.01){
            int x = (int)(Math.random() * (WIDTH - 20));
            objects.add(new GameObject(x,0,"powerup"));
        }

        if (shieldActive && System.currentTimeMillis() - shieldStartTime > SHIELD_DURATION) {
            shieldActive = false;
        }

        if (System.currentTimeMillis() - lastSecondTick >= 1000) {
            timeLeft--;
            timerLabel.setText("Time: " + timeLeft);
            lastSecondTick = System.currentTimeMillis();
            if (timeLeft <= 0) {
                if (level < MAX_LEVEL) {
                    // ── LEVEL PROGRESSION ──
                    level++;
                    timeLeft = 60;               // reset round timer
                    objects.clear();             // clear the screen
                    applyLevelDifficulty();      // faster fish, more spawns
                    showLevelUp = true;       // show banner
                    levelUpStartTime = System.currentTimeMillis();
                } else {
                    // Survived all levels → game over (victory)
                    isGameOver = true;
                }
            }
        }

        if (Math.random() < fishSpawnRate) {
            int x = (int) (Math.random() * (WIDTH - OBSTACLE_WIDTH));
            objects.add(new GameObject(x, 0, "fish"));
        }

        if (Math.random() < puffSpawnRate) {
            int x = (int) (Math.random() * (WIDTH - OBSTACLE_WIDTH));
            objects.add(new GameObject(x, 0, "pufferfish"));
        }


        scoreLabel.setText("Score: " + score + " | Level: " + level);
        healthLabel.setText("Lives: " + lives + " | Health: " + health);

        for (int i = 0; i < objects.size(); i++) {
            objects.get(i).y += currentObstacleSpeed;
            if (objects.get(i).y > HEIGHT) {
                objects.remove(i);
                i--;
            }
        }

        if (Math.random() < 0.03) {
            int x = (int) (Math.random() * (WIDTH - OBSTACLE_WIDTH));
            objects.add(new GameObject(x, 0, "fish"));
        }

        if(Math.random() < 0.005){
            int x = (int)(Math.random() * (WIDTH - OBSTACLE_WIDTH));
            objects.add(new GameObject(x,0,"pufferfish"));
        }

        if (Math.random() < 0.1) {
            stars = generateStars(200);
        }

        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        for(int i = 0; i < objects.size(); i++){
            GameObject object = objects.get(i);
            Rectangle objRect = new Rectangle(object.x, object.y, 20,20);
            //Krish
            if (playerRect.intersects(objRect)) {
                if (object.type.equals("pufferfish")) {
                    if (!isShieldActive()) {
                        health -= 40;
                        if (health <= 0) {
                            lives--;
                            if (lives > 0) health = 100;
                            else           isGameOver = true;
                        }
                    }
                } else if (object.type.equals("powerup")) {
                    if (lives < 3) lives++;
                } else if (object.type.equals("fish")) {
                    score += level * 10;
                }
                objects.remove(i);
                i--;
            }
        }

        for(int i = 0; i < objects.size(); i++){
            objects.get(i).y += currentObstacleSpeed;

            if(objects.get(i).y > HEIGHT){
                objects.remove(i);
                i--;
            }
        }

    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT && playerX > 0) {
            playerX -= PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_WIDTH) {
            playerX += PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            reset();
        } else if (keyCode == KeyEvent.VK_SHIFT) {
            activateShield();
        } else if (keyCode == KeyEvent.VK_SPACE) {
            poleDeployed = !poleDeployed;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FishGame().setVisible(true));
    }
}