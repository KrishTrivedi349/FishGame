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
import javax.sound.sampled.*;


public class FishGame extends JFrame implements KeyListener {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 50;
    private static final int OBSTACLE_WIDTH = 20;
    private static final int OBSTACLE_HEIGHT = 20;
    private static final int PROJECTILE_WIDTH = 5;
    private static final int PROJECTILE_HEIGHT = 10;
    private static final int PLAYER_SPEED = 50;
    private static final int OBSTACLE_SPEED = 3;
    private static final int PROJECTILE_SPEED = 10;

    private int score = 0;
    private int health = 100;
    private int level = 1;
    private int timeLeft = 60;

    private JPanel gamePanel;
    private JLabel scoreLabel;
    private JLabel healthLabel;
    private JLabel timerLabel;

    private Timer timer;
    private boolean isGameOver;

    private int playerX, playerY;
    private int projectileX, projectileY;

    private boolean isProjectileVisible;

    private boolean isFiring;

    private List<Point> obstacles = new ArrayList<>();
    private List<Point> stars = new ArrayList<>();
    private List<Point> healthPacks = new ArrayList<>();

    private BufferedImage shipImage;
    private BufferedImage spriteSheet;

    private int spriteWidth = 64;
    private int spriteHeight = 64;

    private Clip clip;

    private boolean shieldActive = false;
    private int shieldDuration = 5000;
    private long shieldStarTime;

    private long lastSecondTick = System.currentTimeMillis();

    private List<Point> generateStars(int numStars){
        List<Point> starsList = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numStars; i++){
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            starsList.add(new Point(x,y));
        }
        return starsList;
    }

    private void activateShield(){
        shieldActive = true;
        shieldStarTime = System.currentTimeMillis();
    }
    private void deactivateShield(){
        shieldActive = false;
    }

    private boolean isShieldActive(){
        return shieldActive && (System.currentTimeMillis() - shieldStarTime) < shieldDuration;
    }
    private void reset(){
        score = 0;
        isGameOver = false;
        repaint();
    }
    public FishGame() {

        try {
            shipImage = ImageIO.read(new File("ship.png"));
            spriteSheet = ImageIO.read(new File("astro.png"));
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("fire.wav").getAbsoluteFile());
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        } catch (UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }


        setTitle("Space Game");
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

        healthLabel = new JLabel("Health: 100");
        healthLabel.setForeground(Color.RED);
        gamePanel.add(healthLabel);

        timerLabel = new JLabel("Time: 60");
        timerLabel.setForeground(Color.YELLOW);
        gamePanel.add(timerLabel);

        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 20;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;
        isProjectileVisible = false;
        isGameOver = false;
        isFiring = false;
        obstacles = new java.util.ArrayList<>();

        timer = new Timer(20, e -> {
            if (!isGameOver) {
                update();
                repaint();
            }
        });
        timer.start();
    }

    public void playSound(){
        if(clip != null){
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public static Color generateRandomColor(){
        Random rand = new Random();
        int r = rand.nextInt(256);
        int g = rand.nextInt(256);
        int b = rand.nextInt(256);
        return new Color(r,g,b);
    }


    private void draw(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.drawImage(shipImage, playerX, playerY, null);

        //g.setColor(Color.BLUE);
        //g.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        if (isProjectileVisible) {
            g.setColor(Color.GREEN);
            g.fillRect(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        }

        //g.setColor(Color.RED);
        //for (Point obstacle : obstacles) {
        //    g.fillRect(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
        //}

        for(Point obstacle : obstacles){
            if(spriteSheet != null){
                Random random = new Random();
                int spriteIndex = random.nextInt(4);

                int spriteX = spriteIndex * spriteWidth;
                int spriteY = 0;

                g.drawImage(spriteSheet.getSubimage(spriteX,spriteY,spriteWidth,
                        spriteHeight),obstacle.x, obstacle.y, null);
            }
        }

        g.setColor(Color.GREEN);
        for (Point p : healthPacks){
            g.fillRect(p.x, p.y, 15,15);
        }

        g.setColor(generateRandomColor());
        for (Point star : stars){
            g.fillOval(star.x, star.y, 2, 2);
        }

        if (isShieldActive()){
            g.setColor(new Color(0,255,255,100));
            g.fillOval(playerX, playerY, 60,60);
        }

        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Game Over!", WIDTH / 2 - 80, HEIGHT / 2);
        }
    }

    private void update() {

        if (System.currentTimeMillis() - lastSecondTick >= 1000){
            timeLeft--;
            timerLabel.setText("Time: " + timeLeft);
            lastSecondTick = System.currentTimeMillis();
            if (timeLeft <= 0) isGameOver = true;
        }

        if (score >= 100) level = 3;
        else if (score >= 50) level = 2;

        scoreLabel.setText("Score: " + score + " | Level: " + level);
        healthLabel.setText("Health: " + health);

        for (int i = 0; i < obstacles.size(); i++) {
            obstacles.get(i).y += OBSTACLE_SPEED + level;
            if (obstacles.get(i).y > HEIGHT) {
                obstacles.remove(i);
                i--;
            }
        }

        if (Math.random() < 0.02 + level * 0.01) {
            int obstacleX = (int) (Math.random() * (WIDTH - OBSTACLE_WIDTH));
            obstacles.add(new Point(obstacleX, 0));
        }

        if (Math.random() < 0.01){
            healthPacks.add(new Point(new Random().nextInt(WIDTH),0));
        }

        for (Point p : healthPacks) {
            p.y += 2;
        }

        if (Math.random() < 0.1){
            stars = generateStars(200);
        }

        if (isProjectileVisible) {
            projectileY -= PROJECTILE_SPEED;
            if (projectileY < 0) {
                isProjectileVisible = false;
            }
        }

        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle r = new Rectangle(obstacles.get(i).x, obstacles.get(i).y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
            if (playerRect.intersects(r) && !isShieldActive()) {
                health -= 20;
                obstacles.remove(i);
                if (health <= 0) isGameOver = true;
                break;
            }
        }

        for (int i = 0; i < healthPacks.size(); i++){
            Rectangle rect = new Rectangle(healthPacks.get(i).x, healthPacks.get(i).y,15,15);
            if (playerRect.intersects(rect)){
                health = Math.min(100,health + 20);
                healthPacks.remove(i);
                break;
            }
        }

        Rectangle projectileRect = new Rectangle(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle obstacleRect = new Rectangle(obstacles.get(i).x, obstacles.get(i).y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
            if (projectileRect.intersects(obstacleRect)) {
                obstacles.remove(i);
                score += 10;
                isProjectileVisible = false;
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
        } else if (keyCode == KeyEvent.VK_CONTROL){
            activateShield();
        } else if (keyCode == KeyEvent.VK_SPACE && !isFiring) {
            playSound();
            isFiring = true;
            projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
            projectileY = playerY;
            isProjectileVisible = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500); // Limit firing rate
                        isFiring = false;
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FishGame().setVisible(true);
            }
        });
    }
}

