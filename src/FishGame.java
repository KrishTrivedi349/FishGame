import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.net.URL;
public class FishGame extends JFrame implements KeyListener {
    private static final int WIDTH  = 500;
    private static final int HEIGHT = 500;
    private static final int DOCK_Y = 85;
    private static final int PLAYER_WIDTH  = 50;
    private static final int PLAYER_HEIGHT = 50;
    private static final int PLAYER_SPEED  = 25;

    private static final String[] FISH_TYPES = {"shark","trout","goldfish","jellyfish","catfish","starfish"};
    private static final String[] OBSTACLE_TYPES = {"rock","trash"};

    private int currentObstacleSpeed = 3;
    private double fishSpawnRate = 0.03;
    private double puffSpawnRate = 0.005;
    private double obstacleSpawnRate = 0.008;
    private static final int MAX_LEVEL = 3;
    private static final int DISPLAY_MSG = 2000;


    private int score = 0;
    private int health = 100;
    private int lives = 3;
    private int level = 1;
    private int timeLeft = 60;
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private boolean showLevelUp = false;
    private long levelUpStartTime = 0;

    private JPanel gamePanel;
    private JLabel scoreLabel;
    private JLabel healthLabel;
    private JLabel timerLabel;
    private Timer  swingTimer;

    private int playerX, playerY;
    private int poleY;
    private int poleSpeed;
    private boolean poleDeployed = false;
    private final int poleStartY = 90;
    private final int poleEndY = HEIGHT - 20;

    private List<GameObject> objects = new ArrayList<>();
    private GameObject caughtFish = null;

    private ArrayList<int[]> bubbles = new ArrayList<>();

    private BufferedImage yodaImage, exploreImage, alphaImage;
    private BufferedImage currentCatImage;
    private BufferedImage coralReefImage, seaweedImage;
    private BufferedImage rockImage;
    private BufferedImage sodacanImage;
    private BufferedImage sharkImage, troutImage, goldfishImage, jellyfishImage, catfishImage, starfishImage, pufferfishImage;

    private boolean shieldActive = false;
    private long shieldStartTime;
    private long lastShieldUseTime;
    private static final int SHIELD_DURATION = 3000;
    private static final int SHIELD_COOLDOWN = 30000;

    private long lastSecondTick = System.currentTimeMillis();

    public class GameObject {
        int x, y, size;
        int dx;
        String type;

        GameObject(int x, int y, String type, int size, int dx) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.size = size;
            this.dx = dx;
        }
    }

    private void applyLevelDifficulty() {
        currentObstacleSpeed = 3  + (level - 1) * 2;
        fishSpawnRate = 0.03  + (level - 1) * 0.015;
        puffSpawnRate = 0.005 + (level - 1) * 0.004;
        obstacleSpawnRate = 0.008 + (level - 1) * 0.003;
    }

    private void createBubbles() {
        for (int i = 0; i < 50; i++) {
            int x = (int)(Math.random() * WIDTH);
            int y = DOCK_Y + (int)(Math.random() * (HEIGHT - DOCK_Y));
            int size = 5 + (int)(Math.random() * 15);
            bubbles.add(new int[]{x, y, size});
        }
    }

    private void moveBubbles() {
        for (int[] b : bubbles) {
            b[1] -= 1;
            if (b[1] < DOCK_Y) {
                b[1] = HEIGHT;
                b[0] = (int)(Math.random() * WIDTH);
            }
        }
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
        showLevelUp  = false;
        isGameOver = false;
        isVictory = false;
        shieldActive = false;
        poleDeployed = false;
        poleY = poleStartY;
        caughtFish = null;
        objects.clear();
        chooseCat();
        repaint();
    }


    private boolean isFishType(String t){
        for (String f : FISH_TYPES) {
            if (f.equals(t)) {
                return true;
            }
        }
        return false;
    }

    private boolean isObstacleType(String t) {
        for (String o : OBSTACLE_TYPES) {
            if (o.equals(t)) {
                return true;
            }
        }
        return false;
    }

    private String randomFishType(){
        return FISH_TYPES[(int)(Math.random() * FISH_TYPES.length)];
    }

    private String randomObstacleType() {
        return OBSTACLE_TYPES[(int)(Math.random() * OBSTACLE_TYPES.length)];
    }

    private int sizeForType(String t) {
        switch (t) {
            case "shark":
                return 32;
            case "jellyfish":
                return 26;
            case "trout":
                return 24;
            case "pufferfish":
                return 24;
            case "catfish":
                return 22;
            case "starfish":
                return 22;
            case "goldfish":
                return 18;
            case "rock":
                return 26;
            case "trash":
                return 20;
            default:
                return 20;
        }
    }

    private int scoreForType(String t) {
        switch (t) {
            case "goldfish":
                return 10;
            case "trout":
                return 15;
            case "catfish":
                return 20;
            case "starfish":
                return 25;
            case "jellyfish":
                return 30;
            case "shark":
                return 50;
            default:
                return 10;
        }
    }



    public FishGame() {
        try {
            yodaImage = ImageIO.read(new File("src/yoda.png"));
        } catch (IOException ignored) {}

        try {
            exploreImage   = ImageIO.read(new File("src/explore.png"));
        } catch (IOException ignored) {}

        try {
            alphaImage = ImageIO.read(new File("src/alpha.png"));
        } catch (IOException ignored) {}

        try {
            coralReefImage = ImageIO.read(new File("src/coral.png"));
        } catch (IOException ignored) {}

        try {
            seaweedImage   = ImageIO.read(new File("src/seaweed.png"));
        } catch (IOException ignored) {}

        try {
            rockImage = ImageIO.read(getClass().getResource("/rock.png"));
            sodacanImage = ImageIO.read(getClass().getResource("/sodacan copy.png"));

            sharkImage = ImageIO.read(getClass().getResource("/shark.png"));
            troutImage = ImageIO.read(getClass().getResource("/trout.png"));
            goldfishImage = ImageIO.read(getClass().getResource("/goldfish.png"));
            jellyfishImage = ImageIO.read(getClass().getResource("/jellyfish.png"));
            catfishImage = ImageIO.read(getClass().getResource("/catfish.png"));
            starfishImage = ImageIO.read(getClass().getResource("/starfish.png"));
            pufferfishImage = ImageIO.read(getClass().getResource("/pufferfish.png"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        chooseCat();

        setTitle("Underwater Cat Fishing");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g); draw(g);
            }
        };

        scoreLabel = new JLabel("Score: 0 | Level: 1");
        scoreLabel.setForeground(Color.BLUE);
        healthLabel = new JLabel("Lives: 3 | Health: 100");
        healthLabel.setForeground(Color.RED);
        timerLabel = new JLabel("Time: 60");
        timerLabel.setForeground(Color.YELLOW);
        gamePanel.add(scoreLabel);
        gamePanel.add(healthLabel);
        gamePanel.add(timerLabel);

        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = 40;
        poleY = poleStartY;

        createBubbles();
        applyLevelDifficulty();

        swingTimer = new Timer(20, e -> {
            if (!isGameOver) {
                update();
                repaint();
            }
        });
        swingTimer.start();
    }

    private void chooseCat(){
        String[] options = {"Yoda", "Explore", "Alpha"};
        int choice = JOptionPane.showOptionDialog(null, "Choose your cat:", "Cat Selection",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == 0){
            currentCatImage = yodaImage;
            poleSpeed = 3;
        } else if (choice == 1) {
            currentCatImage = exploreImage;
            poleSpeed = 6;
        } else{
            currentCatImage = alphaImage;
            poleSpeed = 9;
        }
    }
    private void draw(Graphics g) {

        g.setColor(new Color(135, 206, 235));
        g.fillRect(0, 0, WIDTH, DOCK_Y);

        g.setColor(new Color(0, 100, 150));
        g.fillRect(0, DOCK_Y, WIDTH, HEIGHT - DOCK_Y);

        drawUnderwaterDecorations(g);

        drawBubbles(g);

        g.setColor(new Color(139, 69, 19));
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


        for (GameObject obj : objects) drawObject(g, obj);
        if (caughtFish != null) drawObject(g, caughtFish);


        g.setColor(Color.GRAY);
        g.fillRect(10, 30, 100, 10);
        if (health > 60){
            g.setColor(Color.GREEN);
        } else if (health > 30) {
            g.setColor(Color.YELLOW);

        } else {
            g.setColor(Color.RED);
        }

        g.fillRect(10, 30, health, 10);
        g.setColor(Color.BLACK);
        g.drawRect(10, 30, 100, 10);


        for(int i = 0; i < lives; i++){
            g.setColor(Color.PINK);
            g.fillOval(10 + (i * 30),50,15,15);
        }

        if (isShieldActive()) {
            g.setColor(new Color(0, 255, 255, 120));
            g.fillOval(playerX - 5, playerY - 5, PLAYER_WIDTH + 10, PLAYER_HEIGHT + 10);
        }


        if (showLevelUp) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(WIDTH/2 - 110, HEIGHT/2 - 40, 220, 70, 20, 20);
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("LEVEL UP!", WIDTH/2 - 70, HEIGHT/2 - 10);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Level " + level + " - Good luck!", WIDTH/2 - 72, HEIGHT/2 + 20);
        }

        if (isGameOver) {
            g.setColor(new Color(0, 0, 0, 190));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setFont(new Font("Arial", Font.BOLD, 38));
            g.setColor(isVictory ? Color.YELLOW : Color.RED);
            g.drawString(isVictory ? "You Win!" : "Game Over!", WIDTH/2 - 95, HEIGHT/2 - 30);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.setColor(Color.WHITE);
            g.drawString("Final Score: " + score, WIDTH/2 - 70, HEIGHT/2 + 10);
            g.drawString("Press ESC to restart",  WIDTH/2 - 95, HEIGHT/2 + 45);
        }
    }


    private void drawUnderwaterDecorations(Graphics g) {

        if (seaweedImage != null) {
            g.drawImage(seaweedImage,45,HEIGHT - 115,35,90,null);
            g.drawImage(seaweedImage,82,HEIGHT - 95,28,70,null);
            g.drawImage(seaweedImage,390,HEIGHT - 120,35,95,null);
            g.drawImage(seaweedImage,425,HEIGHT - 100,28,75,null);
        }

        if (coralReefImage != null) {
            g.drawImage(coralReefImage, 195, HEIGHT - 85, 80, 65, null);
        }


    }


    private void drawBubbles(Graphics g) {
        for (int[] b : bubbles) {
            g.setColor(new Color(173, 216, 230, 140));
            g.fillOval(b[0], b[1], b[2], b[2]);
            g.setColor(new Color(255, 255, 255, 160));
            g.drawOval(b[0], b[1], b[2], b[2]);
        }
    }

    private void drawObject(Graphics g, GameObject obj) {
        int x = obj.x;
        int y = obj.y;
        int s = obj.size;

        switch (obj.type) {

            case "shark":
                if (sharkImage != null)
                    g.drawImage(sharkImage, x, y, s, s, null);
                break;

            case "trout":
                if (troutImage != null)
                    g.drawImage(troutImage, x, y, s, s, null);
                break;

            case "goldfish":
                if (goldfishImage != null)
                    g.drawImage(goldfishImage, x, y, s, s, null);
                break;

            case "jellyfish":
                if (jellyfishImage != null)
                    g.drawImage(jellyfishImage, x, y, s, s, null);
                break;

            case "catfish":
                if (catfishImage != null)
                    g.drawImage(catfishImage, x, y, s, s, null);
                break;

            case "starfish":
                if (starfishImage != null)
                    g.drawImage(starfishImage, x, y, s, s, null);
                break;

            case "pufferfish":
                if (pufferfishImage != null)
                    g.drawImage(pufferfishImage, x, y, s, s, null);
                break;

            case "rock":
                if (rockImage != null)
                    g.drawImage(rockImage, x, y, s, s, null);
                break;

            case "trash":
                if (sodacanImage != null)
                    g.drawImage(sodacanImage, x, y, s, s, null);
                break;

            case "powerup":
                g.setColor(Color.GREEN);
                g.fillOval(x, y, s, s);
                break;
        }
    }

    private void update() {

        if (showLevelUp && System.currentTimeMillis() - levelUpStartTime > DISPLAY_MSG) {
            showLevelUp = false;
        }

        if (caughtFish != null) {
            caughtFish.x = playerX + PLAYER_WIDTH/2 - caughtFish.size/2;
            caughtFish.y = poleY - caughtFish.size/2;
            if (!poleDeployed && poleY <= poleStartY) {
                if (isFishType(caughtFish.type)) {
                    score += scoreForType(caughtFish.type) * level;
                    playSound("splash.wav");
                } else if (caughtFish.type.equals("powerup")) {
                    if (lives < 3) lives++;
                    score += 50;
                }
                caughtFish = null;
            }
        }

        if (poleDeployed) {
            if (poleY < poleEndY){
                poleY += poleSpeed;
            }
        } else {
            if (poleY > poleStartY) {
                poleY -= poleSpeed;
            }
        }

        moveBubbles();


        if (shieldActive && System.currentTimeMillis() - shieldStartTime > SHIELD_DURATION) {
            shieldActive = false;
        }


        if (System.currentTimeMillis() - lastSecondTick >= 1000) {
            timeLeft--;
            timerLabel.setText("Time: " + timeLeft);
            lastSecondTick = System.currentTimeMillis();
            if (timeLeft <= 0) {
                if (level < MAX_LEVEL) {
                    level++;
                    timeLeft = 60;
                    objects.clear();
                    caughtFish = null;
                    applyLevelDifficulty();
                    showLevelUp      = true;
                    levelUpStartTime = System.currentTimeMillis();
                } else {
                    isVictory = true;
                    isGameOver = true;
                }
            }
        }

        scoreLabel.setText("Score: " + score + " | Level: " + level);
        healthLabel.setText("Lives: " + lives + " | Health: " + health);


        for (int i = 0; i < objects.size(); i++) {
            objects.get(i).x += objects.get(i).dx;
            if (objects.get(i).x < -50 || objects.get(i).x > WIDTH + 50) {
                objects.remove(i--);
            }
        }


        if (Math.random() < fishSpawnRate) {
            String t = randomFishType();

            boolean fromLeft = Math.random() < 0.5;

            int x = fromLeft ? -40 : WIDTH;
            int y = DOCK_Y + (int) (Math.random() * (HEIGHT - DOCK_Y - 50));
            int dx = fromLeft ? currentObstacleSpeed : -currentObstacleSpeed;

            objects.add(new GameObject(x, y, t, sizeForType(t), dx));
        }

        if (Math.random() < puffSpawnRate) {
            boolean fromLeft = Math.random() < 0.5;

            int x = fromLeft ? -40 : WIDTH;
            int y = DOCK_Y + (int)(Math.random() * (HEIGHT - DOCK_Y - 50));
            int dx = fromLeft ? currentObstacleSpeed : -currentObstacleSpeed;

            objects.add(new GameObject(x, y, "pufferfish", sizeForType("pufferfish"), dx));
        }


        if (Math.random() < obstacleSpawnRate) {
            String t = randomObstacleType();

            boolean fromLeft = Math.random() < 0.5;

            int x = fromLeft ? -40 : WIDTH;
            int y = DOCK_Y + (int)(Math.random() * (HEIGHT - DOCK_Y - 50));
            int dx = fromLeft ? currentObstacleSpeed : -currentObstacleSpeed;

            objects.add(new GameObject(x, y, t, sizeForType(t), dx));
        }


        if (Math.random() < 0.005) {
            boolean fromLeft = Math.random() < 0.5;

            int x = fromLeft ? -30 : WIDTH;
            int y = DOCK_Y + (int)(Math.random() * (HEIGHT - DOCK_Y - 50));
            int dx = fromLeft ? 2 : -2; // slower so it's easier to catch

            objects.add(new GameObject(x, y, "powerup", 20, dx));
        }


        if (caughtFish == null) {
            int hookCX = playerX + PLAYER_WIDTH/2;
            Rectangle hook = new Rectangle(hookCX - 6, poleY - 6, 12, 12);

            for (int i = 0; i < objects.size(); i++) {
                GameObject obj  = objects.get(i);
                Rectangle  rect = new Rectangle(obj.x, obj.y, obj.size, obj.size);
                if (!hook.intersects(rect)) {
                    continue;
                }

                if (obj.type.equals("pufferfish")) {

                    if (!isShieldActive()) {
                        lives--;
                        playSound("meow.wav");
                        health = 100;
                        if (lives <= 0) {
                            isGameOver = true;
                        }
                    }
                    objects.remove(i--);

                } else if (isObstacleType(obj.type)) {

                    if (!isShieldActive()) {
                        health -= 20;
                        playSound("meow.wav");
                        if (health <= 0) {
                            lives--;
                            if (lives > 0) {
                                health = 100;
                            } else {
                                isGameOver = true;
                            }
                        }
                    }
                    objects.remove(i--);

                } else {
                    caughtFish = obj;
                    poleDeployed = false;
                    objects.remove(i--);
                }
                break;
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
    public void keyTyped(KeyEvent e){}
    private void playSound(String filename) {
        try {

            URL soundUrl = getClass().getResource(filename);

            if (soundUrl == null) {
                System.out.println("⚠️ Sound file not found in classpath: " + filename);


                File soundFile = new File(filename);
                if (!soundFile.exists()) {
                    System.out.println("⚠️ Sound file not found in file system: " + filename);
                    return;
                }
                soundUrl = soundFile.toURI().toURL();
            }


            AudioInputStream originalStream = AudioSystem.getAudioInputStream(soundUrl);
            AudioFormat baseFormat = originalStream.getFormat();


            AudioFormat compatibleFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(compatibleFormat, originalStream);


            Clip clip = AudioSystem.getClip();
            clip.open(convertedStream);
            clip.setFramePosition(0);
            clip.start();


            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    try {
                        originalStream.close();
                        convertedStream.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FishGame().setVisible(true));
    }
}

