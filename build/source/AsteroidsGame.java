import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class AsteroidsGame extends PApplet {

// spacestation health and heal, upgrade stuff, etc.
// E to go warp speed
// Rocket visuals... meh
// Game over text
// Ships decrase health if out of range
// asteroid spacestation collisions

/* Connect processing with browser js */
public interface JavaScript {
  public void playSound(String s);
  public void keyDown(String k);
  public void keyUp(String k);
}
public void bindJavascript(JavaScript js) {
  javascript = js;
}
public JavaScript javascript;

/* Constant variables */
public final int NUM_STARS = 2000;
public final double MY_SHIP_ACCELERATION = 0.1f;
public final double ASTEROID_SPAWN_CHANCE = 10;
public final int MAX_ASTEROIDS = 20;
public final int INITIAL_ENEMIES = 2;
public final int INITIAL_WINGSHIPS = 2;
public final int MAP_WIDTH = 5000;
public final int MAP_HEIGHT = 5000;
public final int OUT_OF_BOUNDS_COLOR = color(50,0,0);
public final int NUM_PARTICLES = 10;
public final int displayWidth = 1100;
public final int displayHeight = 700;
//public final int BOX_KEY_SIZE = 75;

/* Game variables */
public int gameState = 0;
public int score;
public int asteroidsDestroyed;
public int bulletsShot;
public int enemiesDestroyed;
public int scrap = 0;
public boolean titleMusicPlaying = false;
public boolean bgMusicPlaying = false;
public boolean pauseDrawn = false;
public boolean gameOverShown = false;

/* Object variables */
public MyShip myShip;
public ArrayList<WingShip> wingShips = new ArrayList<WingShip>();
public ArrayList<EnemyShip> enemyShips = new ArrayList<EnemyShip>();
public ArrayList<Star> stars = new ArrayList<Star>();
public ArrayList<Asteroid> asteroids = new ArrayList<Asteroid>();
public ArrayList<Bullet> bullets = new ArrayList<Bullet>();
public ArrayList<HealthBar> healthBars = new ArrayList<HealthBar>();
public ArrayList<Particle> particles = new ArrayList<Particle>();
public ArrayList<MyShip> myShips = new ArrayList<MyShip>();
public Spacestation friendlySpacestation;
public Spacestation enemySpacestation;
public Camera camera;
public MiniMap minimap;

/* Other variables */
public HashMap<String,Boolean> keys = new HashMap<String,Boolean>();
public String missionText = "placeholder";

public void setup() {
  /* Set screen size, framerate */
  //fullScreen(P2D);
  
  frameRate(60);

  /* Initialize hashmap keys */
  keys.put("w", false);
  keys.put("s", false);
  keys.put("a", false);
  keys.put("d", false);
  keys.put("q", false);
  keys.put(" ", false);

  /* Initialize objects that WILL NOT change */
  friendlySpacestation = new Spacestation("friendly");
  enemySpacestation = new Spacestation("enemy");
}

/* Manages gamestates */
public void draw() {
  switch(gameState) {
    case 0:
      titleScreen();
      break;
    case 1:
      gameScreen();
      break;
    case 2:
      pauseScreen();
      break;
    case 3:
      gameOverScreen();
      break;
    case 4:
      creditsScreen();
      break;
  }
  // Put this bottom code somewhere else and make it work
  if(myShip.getCurrentHealth() <= 0) {
    myShips.remove(0);
    gameState = 3;
  }
}

public void titleScreen() {
  /* Clear and reset arraylists and variables */
  myShips.clear();
  particles.clear();
  bullets.clear();
  enemyShips.clear();
  asteroids.clear();
  stars.clear();
  wingShips.clear();
  score = 5000; // 0
  asteroidsDestroyed = 0;
  bulletsShot = 0;
  enemiesDestroyed = 0;
  gameOverShown = false;

  /* Initialize objects */
  myShips.add(new MyShip());
  myShip = myShips.get(0);
  camera = new Camera();
  minimap = new MiniMap();
  for(int i = 0; i < NUM_STARS; i++) {
    if(stars.size() <= NUM_STARS) {
      stars.add(new Star());
    }
  }
  for(int i = 0; i < MAX_ASTEROIDS; i++) {
    if(asteroids.size() < MAX_ASTEROIDS) {
      int x = (int)(Math.random()*MAP_WIDTH);
      int y = (int)(Math.random()*MAP_HEIGHT);
      asteroids.add(new Asteroid(x,y));
    }
  }
  for(int i = 0; i < INITIAL_ENEMIES; i++) {
    if(enemyShips.size() < INITIAL_ENEMIES) {
      enemyShips.add(new EnemyShip("scout"));
    }
  }

  for(int i = 0; i < INITIAL_WINGSHIPS; i++) {
    if(wingShips.size() < INITIAL_WINGSHIPS) {
      wingShips.add(new WingShip());
    }
  }

  background(0);
  /* Title screen graphics */
  textSize(16);
  textAlign(CENTER);
  fill(255);
  textSize(32);
  textAlign(RIGHT);
  text("Asteroids and More", width,100);
  textSize(20);
  text("Brandon Lou", width,150);
  text("ENTER to start", width, height);

  /* Title screen music */
  if(titleMusicPlaying == false && javascript != null) {
    javascript.playSound("title");
    titleMusicPlaying = true;
    bgMusicPlaying = false;
  }
}

public void gameScreen() {
  pauseDrawn = false;
  /* Moves camera view */
  translate(-camera.pos.x, -camera.pos.y);
  camera.draw(myShip);

  /* Resets draw screen */
  background(OUT_OF_BOUNDS_COLOR);

  /* Checks which keys are held down and updates accordingly */
  checkKeyValues();
  addEnemies();
  updateCollisions();

  /* Show everything in this order*/
  showSpace();
  friendlySpacestation.show();
  enemySpacestation.show();
  showAsteroids();
  showBullets();
  showSpaceShips();
  showHealthBars();
  showParticles();
  showShip();
  showGUI();
  mouseCursor();

  /* Background music */
  if(bgMusicPlaying == false && javascript != null) {
    javascript.playSound("bg");
    bgMusicPlaying = true;
    titleMusicPlaying = false;
  }
}

public void pauseScreen() {

  if (!pauseDrawn) {

    /* Pause screen graphics setup */
    fill(0,0,0,100);
    stroke(0);
    rect(0,0,displayWidth,displayHeight);
    fill(255);

    /* Pause title */
    textSize(24);
    text("Paused (ESC to unpause).",50,50);
    textSize(20);
    text("Scrap: " + scrap, 50, 75);

    /* Ship upgrades */
    textSize(18);
    text("Ship stats", 50, 125);
    text("10s +2 Max HP", 50, 145);
    text("10s +2 Armor", 50, 165);
    text("15s Energy Lasers Mark II", 50, 185);
    text("10s +10 Max Fuel", 50, 205);
    text("5s +10 Fuel Efficiency", 50, 225);
    text("50s +10 Max Heat", 50, 245);
    text("30s +5 Cooling efficiency", 50, 265);

    /* Ally ship upgrades */
    if(dist(myShip.getX(),myShip.getY(),friendlySpacestation.x,friendlySpacestation.y) < friendlySpacestation.radius) {
      text("10s Build ally ship",400,140);
      text("30s +10 Ally Max HP",400,160);
    }

    /* Spacestation upgrades */
    text("Spacestation Upgrades",50,400);
    text("20s +10 Spacestation health",50,420);
    text("100s Matter Converter Mark II",50,440);

    // Instruction text
    textSize(15);
    text("CONTROLS:",880,40);
    text("W - accelerate",880,60);
    text("A - rotate left",880,80);
    text("S - decellerate",880,100);
    text("D - rotate right",880,120);
    text("SPACE - fire bullets",880,140);
    text("Q - hyperspace",880,160);
    text("E - warp speed",880,180);
    text("ESC - Pause / Upgrade station",880,200);

    pauseDrawn = true;
  }
}

public void gameOverScreen() {

  if(!gameOverShown) {
    for(int i = 0; i < NUM_PARTICLES; i++) {
      particles.add(new Particle(myShip.getX(),myShip.getY(),color(0,0,255)));
    }
    gameOverShown = true;
  }

  translate(-camera.pos.x, -camera.pos.y);
  background(OUT_OF_BOUNDS_COLOR);
  updateCollisions();
  showSpace();
  friendlySpacestation.show();
  enemySpacestation.show();
  showAsteroids();
  showBullets();
  showSpaceShips();
  showHealthBars();
  showParticles();
  showGUI();
  mouseCursor();

  /* Game over graphics */
  noStroke();
  fill(0,0,0,100);
  rect(myShip.getX()-425,myShip.getY()-350,displayWidth,displayHeight);
  strokeWeight(1);
  stroke(255,0,0);
  fill(255,255,255);
  textAlign(LEFT);
  textSize(24);
  text("YOU DIED",myShip.getX()-400,myShip.getY()-300);
  text("ENTER to play again",myShip.getX()-400,myShip.getY()-270);
}

public void creditsScreen() {
  // VERY VERY VERY VERY UNLIKELY
}

/* Switch case when key is pressed that assigns TRUE to a hashmap key
   Only if running processing natively */
public void keyPressed() {
  switch(key) {
    case 'w':
      keys.put("w", true);
      break;
    case 's':
      keys.put("s", true);
      break;
    case 'a':
      keys.put("a", true);
      break;
    case 'd':
      keys.put("d", true);
      break;
    case ' ':
      keys.put(" ", true);
      break;
    case TAB:
      if(gameState == 1) {
        gameState = 2;
      } else if (gameState == 2) {
        gameState = 1;
      }
      break;
  }
}

/* Javascript to processing key function*/
public void keyDown(String k) {
  switch(k) {
    case "w":
      keys.put("w",true);
      break;
    case "a":
      keys.put("a",true);
      break;
    case "s":
      keys.put("s",true);
      break;
    case "d":
      keys.put("d",true);
      break;
    case " ":
      keys.put(" ", true);
      break;
  }
}
public void keyUp(String k) {
  switch (k) {
    case "w":
      keys.put("w",false);
    break;
    case "a":
      keys.put("a",false);
    break;
    case "s":
      keys.put("s",false);
    break;
    case "d":
      keys.put("d",false);
    break;
    case "q":
    if(hasFuel() && myShip.getCurrentFuel() > 0) {
      myShip.hyperspace();
      camera.hyperspace(myShip);
    } break;
    case " ":
      keys.put(" ", false);
      break;
    case "ENTER":
      if(gameState == 3) {
        gameState = 0;
      } else if (gameState == 0) {
        gameState = 1;
      }
      break;
    case "ESC":
      if(gameState == 1) {
        gameState = 2;
      } else if (gameState == 2) {
        gameState = 1;
      }
      break;
  }
}

/* NATIVE PROCESSING KEY FUNCTIONS */
/* Switch case when key is released that assigns FALSE to a hashmap key */
public void keyReleased() {
  switch(key) {
    case 'w':
      keys.put("w", false);
    case 's':
      keys.put("s", false);
      break;
    case 'a':
      keys.put("a", false);
      break;
    case 'd':
      keys.put("d", false);
      break;
    case ' ':
      keys.put(" ", false);
      break;
    case 'q':
      if(hasFuel() && myShip.getCurrentHealth() > 0) {
        myShip.hyperspace();
        camera.hyperspace(myShip);
      } break;
    case ENTER:
      if(gameState == 3) {
        gameState = 0;
      } else if (gameState == 0) {
        gameState = 1;
      }
      break;
  }
}

/* Runs through hashmap and moves ship accordingly */
public void checkKeyValues() {
  if (keys.get("w") == true && hasFuel()) {
    myShip.accelerate(MY_SHIP_ACCELERATION);
    myShip.setCurrentFuel(-0.02f);
  }
  if (keys.get("s") == true && hasFuel()) {
    myShip.accelerate(-(MY_SHIP_ACCELERATION));
    myShip.setCurrentFuel(-0.02f);
  }
  if (keys.get("a") == true) {
    myShip.rotate(-3);
  }
  if (keys.get("d") == true) {
    myShip.rotate(3);
  }
  if (keys.get(" ") == true) {
      bullets.add(new Bullet(myShip,"mine"));
      myShip.setCurrentHeat(0.05f);
      bulletsShot++;
      myShip.recoil();
  }
}

public void showSpaceShips() {
  for(int e = enemyShips.size()-1; e >= 0; e--) {
    enemyShips.get(e).move();
    enemyShips.get(e).show();
    if (dist(enemyShips.get(e).getX(), enemyShips.get(e).getY(), myShip.getX(), myShip.getY()) <= 1000 && (int)(Math.random()*10) == 0) {
      /* Randomly shoots */
      if(enemyShips.get(e).type == "boss") {bullets.add(new Bullet(enemyShips.get(e), "enemy_boss"));}
      else {bullets.add(new Bullet(enemyShips.get(e), "enemy"));}
    }
  }
  for(int w = wingShips.size()-1; w >= 0; w--) {
    wingShips.get(w).move();
    wingShips.get(w).show();
    if(wingShips.get(w).isInRange() && (int)(Math.random()*10) == 0) {
      bullets.add(new Bullet(wingShips.get(w), "friendly"));
    }
  }
}

public void showAsteroids() {
  /* Randomly adds more asteroids */
  if ((int)(Math.random()*ASTEROID_SPAWN_CHANCE) == 0 && asteroids.size() <= MAX_ASTEROIDS) {
    asteroids.add(new Asteroid());
  }
  for(int a = asteroids.size()-1; a >= 0; a--) {
    asteroids.get(a).move();
    asteroids.get(a).show();
  }
}

public void showSpace() {
  /* Draws black space rect and shows the stars */
  fill(0);
  strokeWeight(1);
  stroke(0);
  rect(0,0,MAP_WIDTH,MAP_HEIGHT);
  for(int i = 0; i < stars.size(); i++) {
    stars.get(i).show();
  }
}

/* Moves and shows bullets */
public void showBullets() {
  for (int b = bullets.size() -1; b >= 0; b--) {
    bullets.get(b).move();
    bullets.get(b).show();
  }
}

/* Updates, moves, and shows your spaceship */
public void showShip() {
  myShip.move();
  myShip.show();
}

public void updateCollisions() {
  /* asteroids out of bounds gets removed, asteroids hits ship gets removed and particle effect
  asteroids hits bullet removes asteroid and bullet. particle effect
  bullets out of bound or lose energy gets removed
  bullets hits ship or ship hits bullet
  enemyships remove if dead, wingship remove if dead
  reduce health if out of bounds
  Cool down ship
  Over heat ends life Spacestation heals spaceship
  */

  bulletloop:
  for(int b = bullets.size()-1; b >= 0; b--) {
    /* Hits asteroid */
    asteroidloop:
    for(int a = asteroids.size()-1; a >= 0; a--) {
      if(dist(asteroids.get(a).getX(),asteroids.get(a).getY(),bullets.get(b).getX(),bullets.get(b).getY()) <= 20) {
        bullets.remove(b);
        for(int i = 0; i < NUM_PARTICLES; i++) {
          particles.add(new Particle(asteroids.get(a).getX(),asteroids.get(a).getY(),color(255,127,80)));
        }
        asteroids.remove(a);
        break bulletloop;
      }
      /* Asteroids out of bounds */
      if(asteroids.get(a).getX() > MAP_WIDTH || asteroids.get(a).getX() < 0 || asteroids.get(a).getY() > MAP_HEIGHT || asteroids.get(a).getY() < 0 || asteroids.get(a).getRotationSpeed() == 0) {
        asteroids.remove(a);
        break asteroidloop;
      }
      /* Asteroid hits ship */
      if (dist(asteroids.get(a).getX(), asteroids.get(a).getY(),myShip.getX(), myShip.getY()) <= 20) {
        for(int i = 0; i < NUM_PARTICLES; i++) {
          particles.add(new Particle(asteroids.get(a).getX(),asteroids.get(a).getY(),color(255,127,80)));
        }
        asteroids.remove(a);
        myShip.setCurrentHealth(-myShip.getCurrentHealth());
        myShip.setCurrentFuel(-myShip.getCurrentFuel());
        myShip.setCurrentHeat(-myShip.getCurrentHeat());
        myShips.remove(0);
        gameState = 3;
        break asteroidloop;
      }
    }
    /* Out of bounds or runs out of power */
    if(dist(bullets.get(b).getInitX(),bullets.get(b).getInitY(),bullets.get(b).getX(),bullets.get(b).getY()) >= 700) {
      bullets.remove(b);
      break bulletloop;
    }
    /* Hits your ship */
    if(dist(bullets.get(b).getX(), bullets.get(b).getY(), myShip.getX(), myShip.getY()) <= 20 && bullets.get(b).getType() == "enemy") {
      bullets.remove(b);
      myShip.setCurrentHealth(-0.5f);
      break bulletloop;
    }
    /* Hits enemyship */
    for(int e = enemyShips.size()-1; e >= 0; e--) {
      if(dist(bullets.get(b).getX(), bullets.get(b).getY(), enemyShips.get(e).getX(), enemyShips.get(e).getY()) <= 20 && (bullets.get(b).getType() == "mine" || bullets.get(b).getType() == "friendly")) {
        bullets.remove(b);
        enemyShips.get(e).setCurrentHealth(-0.5f);
        break bulletloop;
      }
      /* Remove if dead */
      if (enemyShips.get(e).getCurrentHealth() <= 0) {
        for(int i = 0; i < NUM_PARTICLES; i++) {
          particles.add(new Particle(enemyShips.get(e).getX(),enemyShips.get(e).getY(),color(255,0,0)));
        }
        enemyShips.remove(e);
        score += 5;
        if(javascript != null) javascript.playSound("explode");
      }
    }
    /* Hits wingship */
    for(int w = wingShips.size()-1; w >= 0; w--) {
      if(dist(wingShips.get(w).getX(),wingShips.get(w).getY(),bullets.get(b).getX(),bullets.get(b).getY()) <= 20 && bullets.get(b).getType() == "enemy") {
        bullets.remove(b);
        wingShips.get(w).setCurrentHealth(-0.5f);
        break bulletloop;
      }
      if(wingShips.get(w).getCurrentHealth() <= 0) {
        for(int i = 0; i < NUM_PARTICLES; i++) {
          particles.add(new Particle(wingShips.get(w).getX(),wingShips.get(w).getY(),color(0,0,255)));
        }
        wingShips.remove(w);
      }
    }
  }
  /* Reduce health if out of bounds */
  if(myShip.getX() < 0 || myShip.getX() > MAP_WIDTH || myShip.getY() < 0 || myShip.getY() > MAP_HEIGHT) {
    myShip.setCurrentHealth(-0.1f);
    if(myShip.getCurrentHealth()<=0){myShip.setCurrentFuel(-myShip.getCurrentFuel());myShip.setCurrentHeat(-myShip.getCurrentHeat());}
  }
  /* Cool down ship */
  if(keys.get(" ") == false) {
    myShip.setCurrentHeat(-0.3f);
  }
  /* Over heat ends life */
  if(myShip.getCurrentHeat() >= myShip.getMaxHeat()) {
    myShip.setCurrentHealth(-myShip.getCurrentHealth());
    myShip.setCurrentFuel(-myShip.getCurrentFuel());
    myShips.remove(0);
    gameState = 3;
  }
  /* Spacestation heals spaceship*/
  if(dist(myShip.getX(), myShip.getY(), friendlySpacestation.x, friendlySpacestation.y) <= friendlySpacestation.radius && myShip.getCurrentHealth() >= 0) {
    myShip.setCurrentHealth(0.1f);
    myShip.setCurrentFuel(1);
  }
}

public void showGUI() {

  /* Render minimap */
  minimap.render();
  textSize(15);

  /* Draw gray sidebar */
  strokeWeight(1);
  stroke(255);
  fill(100,100);
  rect(myShip.getX()+425,myShip.getY()-400,275,1050);

  /* Health text */
  fill(255);
  textAlign(LEFT);
  text("Health",myShip.getX()+450,myShip.getY()-90);

  /* Health bar */
  strokeWeight(1);
  stroke(255);
  fill(255,0,0);

  if(myShip.getCurrentHealth() > 0) {
  rect(myShip.getX()+450,
       myShip.getY()-80,
       (float)(myShip.getCurrentHealth()*(200/myShip.getMaxHealth())),
       10);
  } else {
    rect(myShip.getX()+450,
         myShip.getY()-80,
         0,
         10);
  }

 /* Fuel text */
 fill(255);
 textAlign(LEFT);
 text("Fuel",myShip.getX()+450,myShip.getY()-50);

  /* Fuel bar */
  strokeWeight(1);
  stroke(255);
  fill(0,255,0);
  rect(myShip.getX()+450,
       myShip.getY()-40,
       (float)(myShip.getCurrentFuel()*(200/myShip.getMaxFuel())),
       10);

   /* Speed text */
   fill(255);
   textAlign(LEFT);
   text("Speed",myShip.getX()+450,myShip.getY()-10);

   /* Speed bar */
   strokeWeight(1);
   stroke(255);
   fill(255,255,0);
   rect(myShip.getX()+450,
        myShip.getY()-0,
        (float)(myShip.getCurrentSpeed()*(200/myShip.getMaxSpeed())),
        10);

  /* Heat text */
  fill(255);
  textAlign(LEFT);
  text("Heat",myShip.getX()+450,myShip.getY()+30);

  /* Heat bar */
  strokeWeight(1);
  stroke(255);
  fill(0,0,255);
  rect(myShip.getX()+450,
       myShip.getY()+40,
       (float)(myShip.getCurrentHeat()*(200/myShip.getMaxHeat())),
       10);

  /* Sidebar stats */
  fill(255);
  textAlign(LEFT);
  text("Score: " + score,myShip.getX()+450,myShip.getY()+70);
  text("Scrap: " + scrap,myShip.getX()+450,myShip.getY()+90);

  /* Warning messages */
  textAlign(CENTER);
  if(myShip.getCurrentHeat() >= myShip.getMaxHeat()*0.75f) fill(0,0,255); else fill(0,0,255);
  rect(myShip.getX()+450,myShip.getY()+120,200,50);
  fill(255);
  text("OVERHEATING!",myShip.getX()+550,myShip.getY()+145);
  fill(0,255,0);
  rect(myShip.getX()+450,myShip.getY()+180,200,50);
  fill(255);
  text("FUEL LOW",myShip.getX()+550,myShip.getY()+205);
  fill(255,0,0);
  rect(myShip.getX()+450,myShip.getY()+240,200,50);
  fill(255);
  text("HEALTH LOW",myShip.getX()+550,myShip.getY()+265);

  /* Stats */
  textAlign(LEFT);
  fill(255);
  text("Max health: " + myShip.getMaxHealth(),myShip.getX()-400,myShip.getY()-325);
  text("Armor: ",myShip.getX()-400,myShip.getY()-300);
  text("Laser power: ",myShip.getX()-400,myShip.getY()-275);

  text("Max fuel: " + myShip.getMaxFuel(),myShip.getX()-200,myShip.getY()-325);
  text("Fuel efficency: ",myShip.getX()-200,myShip.getY()-300);

  text("Max heat: " + myShip.getMaxHeat(),myShip.getX(),myShip.getY()-325);

  text("Ally ships: " + wingShips.size(),myShip.getX()-400,myShip.getY()+325);
  text("Ally health: " + wingShips.size(),myShip.getX()-300,myShip.getY()+325);
  text("Spacestation Health: " + friendlySpacestation.getMaxHealth(),myShip.getX()-200,myShip.getY()+325);
  text("Matter converter efficency: " + friendlySpacestation.getMaxHealth(),myShip.getX()-25,myShip.getY()+325);

  /* Pause */
  text("ESC to pause",myShip.getX()+450,myShip.getY()+325);
}

public void addEnemies() {
  if(score <= 20) {
    if((int)random(0,200) == 0) enemyShips.add(new EnemyShip("scout"));

  } else if (score <= 50) {
    if((int)random(0,175) == 0) enemyShips.add(new EnemyShip("scout"));
    if((int)random(0,300) == 0) enemyShips.add(new EnemyShip("adv"));

  } else if (score <= 200) {
    if((int)random(0,175) == 0) enemyShips.add(new EnemyShip("scout"));
    if((int)random(0,275) == 0) enemyShips.add(new EnemyShip("adv"));

  } else if (score <= 500) {
    if((int)random(0,175) == 0) enemyShips.add(new EnemyShip("scout"));
    if((int)random(0,275) == 0) enemyShips.add(new EnemyShip("adv"));
    if((int)random(0,500) == 0) enemyShips.add(new EnemyShip("captain"));

  } else if (score <= 1000) {
    if((int)random(0,175) == 0) enemyShips.add(new EnemyShip("scout"));
    if((int)random(0,275) == 0) enemyShips.add(new EnemyShip("adv"));
    if((int)random(0,400) == 0) enemyShips.add(new EnemyShip("captain"));

  } else if (score <= 5000) {
    if((int)random(0,175) == 0) enemyShips.add(new EnemyShip("scout"));
    if((int)random(0,275) == 0) enemyShips.add(new EnemyShip("adv"));
    if((int)random(0,300) == 0) enemyShips.add(new EnemyShip("captain"));
    if((int)random(0,500) == 0) enemyShips.add(new EnemyShip("boss"));
  } else {
    if((int)random(0,100) == 0) enemyShips.add(new EnemyShip("scout"));
    if((int)random(0,250) == 0) enemyShips.add(new EnemyShip("adv"));
    if((int)random(0,450) == 0) enemyShips.add(new EnemyShip("captain"));
    if((int)random(0,450) == 0) enemyShips.add(new EnemyShip("boss"));
  }
}

public void showHealthBars() {
  healthBars.clear();
  /* Update health bars */
  for (int e = 0; e < enemyShips.size(); e++) {
    if(enemyShips.get(e).getCurrentHealth() != enemyShips.get(e).getMaxHealth())
    healthBars.add(new HealthBar(enemyShips.get(e).getX(),enemyShips.get(e).getY(),enemyShips.get(e).getMaxHealth(),enemyShips.get(e).getCurrentHealth()));
  }
  for (int w = 0; w < wingShips.size(); w++) {
    if(wingShips.get(w).getCurrentHealth() != wingShips.get(w).getMaxHealth())
    healthBars.add(new HealthBar(wingShips.get(w).getX(),wingShips.get(w).getY(),wingShips.get(w).getMaxHealth(),wingShips.get(w).getCurrentHealth()));
  }

  /* Show health bars */
  for(int h = 0; h < healthBars.size(); h++) {
    healthBars.get(h).show();
  }
}

public void showParticles() {
  for(int s = particles.size()-1; s >= 0; s--) {
    particles.get(s).move();
    particles.get(s).show();
    if(dist((float)particles.get(s).initX,(float)particles.get(s).initY,particles.get(s).getX(),particles.get(s).getY()) >= 100) {
      particles.remove(s);
    }
  }
  if(keys.get("w") == true) {
    //cool stuff
    fill(255,0,0);
    double dRadians = myShip.getPointDirection() * (Math.PI/180);
    rect((float)(myShip.getX() - 20*Math.cos(dRadians)),(float)(myShip.getY() - 20*Math.sin(dRadians)),10,10);
  }
}

public void mouseClicked() {
  if(mouseX > 875 && mouseX < 1075 && mouseY > 25 && mouseY < 225) {
    double x  = mouseX - 875; // Gets values down to a range of 0 - 200
    double y = mouseY - 25;
    x *= 25; // Scale factor from 200 minimap size to 5000 map size
    y *= 25;
    myShip.teleport(x,y);
    camera.hyperspace(myShip);
  }
}

public void mouseCursor() {
  stroke(255);
  fill(255,105,180,100);
  int originX = myShip.getX() - 425;
  int originY = myShip.getY() - 350;
  ellipse(originX+mouseX, originY+mouseY,20,20);
}

/* Helpers */
public boolean hasFuel() {
  return(myShip.getCurrentFuel()>0);
}
public boolean isAlive() {
  return (myShip.getCurrentHealth()>0);
}
public class Asteroid extends Floater {

  public int rotationSpeed;

  public Asteroid() {
    corners = 4;
    int[] xC = {10,-10,-10,10};
    int[] yC = {-10,-10,10,10};
    xCorners = xC;
    yCorners = yC;
    myPointDirection = 0;
    strokeColor = color(255,127,80);
    fillColor = color(0,0,0,0);
    rotationSpeed = (int)(Math.random()*8-4);

    /* 4 cases for asteroids to spawn in */
    int startPos = (int)(Math.random()*4+1);
    switch (startPos) {
      case 1:
        myCenterX = (int)(Math.random()*MAP_WIDTH);
        myCenterY = 0;
        myDirectionX = (double)(Math.random()*6-3);
        myDirectionY = (double)(Math.random()*3+1);
        break;
      case 2:
        myCenterX = (int)(Math.random()*MAP_WIDTH);
        myCenterY = MAP_HEIGHT;
        myDirectionX = (double)(Math.random()*6-3);
        myDirectionY = (double)(-(Math.random()*3+1));
        break;
      case 3:
        myCenterX = 0;
        myCenterY = (int)(Math.random()*MAP_HEIGHT);
        myDirectionX = (double)(Math.random()*3+1);
        myDirectionY = (double)(Math.random()*6-1);
        break;
      case 4:
        myCenterX = MAP_WIDTH;
        myCenterY = (int)(Math.random()*MAP_HEIGHT);
        myDirectionX = (double)(-(Math.random()*6));
        myDirectionY = (double)(Math.random()*6-3);
        break;
    }
  }

  public Asteroid(int x, int y) {
    corners = 4;
    int[] xC = {10,-10,-10,10};
    int[] yC = {-10,-10,10,10};
    xCorners = xC;
    yCorners = yC;
    myPointDirection = 0;
    strokeColor = color(255,127,80);
    fillColor = color(0,0,0);
    rotationSpeed = (int)(Math.random()*8-4);
    myCenterX = x;
    myCenterY = y;
    myDirectionX = (double)(Math.random()*6-3);
    myDirectionY = (double)(Math.random()*3);
  }

  public void setX(int x){myCenterX = x;}
  public int getX(){return (int)myCenterX;}
  public void setY(int y){myCenterY = y;}
  public int getY(){return (int)myCenterY;}
  public void setDirectionX(double x){myDirectionX = x;}
  public double getDirectionX(){return myDirectionX;}
  public void setDirectionY(double y){myDirectionY = y;}
  public double getDirectionY(){return myDirectionY;}
  public void setPointDirection(int degrees){myPointDirection = degrees;}
  public double getPointDirection(){return myPointDirection;}
  public int getRotationSpeed(){return rotationSpeed;}

  public void move() {
    super.move();
    rotate(rotationSpeed);
  }

}
public class Bullet extends Floater {
  private String type;
  private double initX, initY;
  public Bullet(SpaceShip ship, String t) {
    corners = 4;
    int[] xC = {2,2,-2,-2};
    int[] yC = {1,-1,-1,1};
    xCorners = xC;
    yCorners = yC;
    myCenterX = ship.getX();
    myCenterY = ship.getY();
    this.initX = ship.getX();
    this.initY = ship.getY();
    myPointDirection = ship.getPointDirection() + Math.random()*5-2.5f;
    double dRadians = myPointDirection * (Math.PI/180);
    myDirectionX = 20 * Math.cos(dRadians) + ship.getDirectionX();
    myDirectionY = 20 * Math.sin(dRadians) + ship.getDirectionY();
    switch (t) {
      case "mine":
        type = t;
        fillColor = color(0,191,255);
        strokeColor = color(0,191,255);
        break;
      case "friendly":
        type = t;
        fillColor = color(0,191,255);
        strokeColor = color(0,191,255);
        break;
      case "enemy":
        type = t;
        fillColor = color(255,0,0);
        strokeColor = color(255,0,0);
        break;
      case "enemy_boss":
        type = t;
        fillColor = color(0,255,0);
        strokeColor = color(0,255,0);
      break;
    }
  }
  public void setX(int x){myCenterX = x;}
  public int getX(){return (int)myCenterX;}
  public void setY(int y){myCenterY = y;}
  public int getY(){return (int)myCenterY;}
  public int getInitX(){return (int)initX;}
  public int getInitY(){return (int)initY;}
  public void setDirectionX(double x){myDirectionX = x;}
  public double getDirectionX(){return myDirectionX;}
  public void setDirectionY(double y){myDirectionY = y;}
  public double getDirectionY(){return myDirectionY;}
  public void setPointDirection(int degrees){myPointDirection = degrees;}
  public double getPointDirection(){return myPointDirection;}
  public String getType(){return type;}

  public void show() {
    fill(fillColor);
    strokeWeight(3);
    stroke(strokeColor);
    ellipse((float)myCenterX, (float)myCenterY, 2, 2);
  }
}
public class Camera {
  public PVector pos;
  public Camera() {
    pos = new PVector(myShip.getX() - 425, myShip.getY() - 350);
  }

  public void draw(MyShip ship) {
    pos.x += ship.getDirectionX();
    pos.y += ship.getDirectionY();
  }

  public void hyperspace(MyShip ship) {
    pos.x = ship.getX() - 425;
    pos.y = ship.getY() - 350;
  }
}
public class EnemyShip extends SpaceShip {

  private double maxHealth,currentHealth;
  private double ACCELERATION;
  public String type;

  public EnemyShip(String t) {
    corners = 11;
    int[] xC = {-14,-10,-12,2,4,14,4,2,-12,-10,-14};
    int[] yC = {-4,-4,-12,-12,-2,0,2,12,12,4,4};
    xCorners = xC;
    yCorners = yC;
    switch(t) {
      case "scout":
        type = t;
        fillColor = color(255,0,0);
        strokeColor = color(255,255,255);
        myCenterX = random(4350,4850);
        myCenterY = random(4350,4850);
        myDirectionX = 0;
        myDirectionY = 0;
        myPointDirection = 0;
        maxHealth = 5;
        currentHealth = 5;
        MAX_VELOCITY = random(2.0f, 4.0f);
        ACCELERATION = random(0.01f, 0.05f);
        break;
      case "adv":
        type = t;
        fillColor = color(150,0,0);
        strokeColor = color(255,255,255);
        myCenterX = random(4350,4850);
        myCenterY = random(4350,4850);
        myDirectionX = 0;
        myDirectionY = 0;
        myPointDirection = 0;
        maxHealth = 10;
        currentHealth = 10;
        MAX_VELOCITY = random(3.0f, 5.0f);
        ACCELERATION = random(0.03f, 0.07f);
        break;
      case "captain":
        type = t;
        fillColor = color(102,0,102);
        strokeColor = color(255,255,255);
        myCenterX = random(4350,4850);
        myCenterY = random(4350,4850);
        myDirectionX = 0;
        myDirectionY = 0;
        myPointDirection = 0;
        maxHealth = 10;
        currentHealth = 10;
        MAX_VELOCITY = random(3.0f, 5.0f);
        ACCELERATION = random(0.03f, 0.07f);
        break;
      case "boss":
        type = t;
        fillColor = color(75,0,130);
        strokeColor = color(255,255,255);
        myCenterX = random(4350,4850);
        myCenterY = random(4350,4850);
        myDirectionX = 0;
        myDirectionY = 0;
        myPointDirection = 0;
        maxHealth = 50;
        currentHealth = 50;
        MAX_VELOCITY = 2.0f;
        ACCELERATION = 0.25f;
        break;
    }
  }

  public double getMaxHealth() {return maxHealth;}
  public double getCurrentHealth(){return currentHealth;}
  public void setCurrentHealth(double ch){currentHealth += ch;}

  public void move() {

    /* Point direction */
    double deltaX, deltaY, angle;
    deltaX = myShip.getX() - myCenterX;
    deltaY = myShip.getY() - myCenterY;

    angle = atan((float)(deltaY/deltaX)) * (180/Math.PI);

    if(myShip.getX() < myCenterX) {
      angle += 180;
    }

    myPointDirection = angle;

    /* Accelerate in point direction */
    accelerate(ACCELERATION);

    /* Limit velocity */
    if(myDirectionX > MAX_VELOCITY) {
      myDirectionX = MAX_VELOCITY;
    }
    if(myDirectionX < -(MAX_VELOCITY)) {
      myDirectionX = -(MAX_VELOCITY);
    }
    if(myDirectionY > MAX_VELOCITY) {
      myDirectionY = MAX_VELOCITY;
    }
    if(myDirectionY < -(MAX_VELOCITY)) {
      myDirectionY = -(MAX_VELOCITY);
    }

    /* Actually move it */
    myCenterX += myDirectionX;
    myCenterY += myDirectionY;

  }
}
public abstract class Floater {
  protected int corners;  //the number of corners, a triangular floater has 3
  protected int[] xCorners;
  protected int[] yCorners;
  protected int strokeColor;
  protected int fillColor;
  protected double myCenterX, myCenterY; //holds center coordinates
  protected double myDirectionX, myDirectionY; //holds x and y coordinates of the vector for direction of travel
  protected double myPointDirection; //holds current direction the ship is pointing in degrees
  abstract public void setX(int x);
  abstract public int getX();
  abstract public void setY(int y);
  abstract public int getY();
  abstract public void setDirectionX(double x);
  abstract public double getDirectionX();
  abstract public void setDirectionY(double y);
  abstract public double getDirectionY();
  abstract public void setPointDirection(int degrees);
  abstract public double getPointDirection();

  //Accelerates the floater in the direction it is pointing (myPointDirection)
  public void accelerate (double dAmount) {
    //convert the current direction the floater is pointing to radians
    double dRadians = myPointDirection * (Math.PI/180);
    //change coordinates of direction of travel
    myDirectionX += ((dAmount) * Math.cos(dRadians));
    myDirectionY += ((dAmount) * Math.sin(dRadians));
  }

  public void rotate (int nDegreesOfRotation) {
    //rotates the floater by a given number of degrees
    myPointDirection += nDegreesOfRotation;
  }
  public void move() { //move the floater in the current direction of travel
    //change the x and y coordinates by myDirectionX and myDirectionY
    myCenterX += myDirectionX;
    myCenterY += myDirectionY;
  }
  public void show()  //Draws the floater at the current position
  {
    strokeWeight(1);
    fill(fillColor);
    stroke(strokeColor);
    //convert degrees to radians for sin and cos
    double dRadians = myPointDirection*(Math.PI/180);
    int xRotatedTranslated, yRotatedTranslated;
    beginShape();
    for(int nI = 0; nI < corners; nI++) {
      //rotate and translate the coordinates of the floater using current direction
      xRotatedTranslated = (int)((xCorners[nI]* Math.cos(dRadians)) - (yCorners[nI] * Math.sin(dRadians))+myCenterX);
      yRotatedTranslated = (int)((xCorners[nI]* Math.sin(dRadians)) + (yCorners[nI] * Math.cos(dRadians))+myCenterY);
      vertex(xRotatedTranslated,yRotatedTranslated);
    }
    endShape(CLOSE);
  }
}
public class HealthBar {
  private double x,y,maxWidth,myHeight,currentWidth,scaleFactor;
  private int fillColor,strokeColor;

  public HealthBar(double x, double y, double maxHealth, double currentHealth) {
    this.x = x - 25;
    this.y = y - 25;
    this.maxWidth = 50;
    this.scaleFactor = maxWidth / maxHealth;
    this.currentWidth = currentHealth * scaleFactor;
    this.fillColor = color(0,255,0); /* green */
    this.myHeight = 7;
  }

  public void show() {
    noStroke();
    fill(fillColor);
    rect((float)this.x, (float)this.y, (float)this.currentWidth, (float)this.myHeight);

    stroke(255);
    fill(0,0,0,0);
    rect((float)this.x, (float)this.y, (float)this.maxWidth, (float)this.myHeight);
  }
}
public class MiniMap {
  public MiniMap() {}
  public void render() {
    /* Map Background */
    stroke(255);
    fill(0);
    rect(myShip.getX()+450,myShip.getY()-325,200,200);

    /* Your spaceship on the map */
    stroke(255,255,0);
    fill(255,255,0);
    if(inRange(myShip)) ellipse(myShip.getX()+450+myShip.getX()/(MAP_WIDTH/200),myShip.getY()-325+myShip.getY()/(MAP_HEIGHT/200),5,5);

    /* Enemyships on the map */
    for(int e = enemyShips.size()-1; e >= 0; e--) {
      stroke(255,0,0);
      fill(255,0,0);
      if(inRange(enemyShips.get(e))) ellipse(myShip.getX()+450+enemyShips.get(e).getX()/(MAP_WIDTH/200),myShip.getY()-325+enemyShips.get(e).getY()/(MAP_HEIGHT/200),5,5);
    }

    /* Wingships on the map */
    for(int w = wingShips.size()-1; w >= 0; w--) {
      stroke(0,191,255);
      fill(0,191,255);
      if(inRange(wingShips.get(w))) ellipse(myShip.getX()+450+wingShips.get(w).getX()/(MAP_WIDTH/200),myShip.getY()-325+wingShips.get(w).getY()/(MAP_HEIGHT/200),5,5);
    }

    /* Asteroids on the map */
    stroke(255,127,80);
    fill(255,127,80);
    for(int a = asteroids.size()-1; a >= 0; a--) {
      ellipse(myShip.getX()+450+asteroids.get(a).getX()/(MAP_WIDTH/200),
              myShip.getY()-325+asteroids.get(a).getY()/(MAP_HEIGHT/200),
              1,1);
    }
    /* HOW THIS THING WORKS:
    For each ship/asteroid, move it to upper left corner of minimap, then add the value/scale factor
    */
  }
  private boolean inRange(SpaceShip ship) {
    if(ship.getX() > 0 && ship.getX() < MAP_WIDTH && ship.getY() > 0 && ship.getY() < MAP_HEIGHT) {
      return true;
    } else {
      return false;
    }
  }
}
/*
public class Missions {
  private int currentLevel;
  private String missionDescription;

  private HashMap<Integer,String> lv1missions;
  private HashMap<Integer,String> lv2missions;
  private HashMap<Integer,String> lv3missions;
  private HashMap<Integer,String> lv4missions;
  private HashMap<Integer,String> lv5missions;

  public Missions() {
    lv1missions = new HashMap<Integer,String>();
    lv2missions = new HashMap<Integer,String>();
    lv3missions = new HashMap<Integer,String>();
    lv4missions = new HashMap<Integer,String>();
    lv5missions = new HashMap<Integer,String>();

    lv1missions.put(10, "Destroy 2 enemy ships."); // 2 scout enemies

    lv2missions.put(20, "Destroy 5 enemy ships."); // 5 scout enemies
    lv2missions.put(21, "Remove 5 asteroids.\nDestroy 3 enemy ships."); // 3 scouts
    lv2missions.put(22, "Destroy enemy construction."); // gaurded by 2 ships.

    lv3missions.put(30, "Destroy 10 enemy ships."); // 9 scouts, 1 adv
    lv3missions.put(31, "Destroy enemy outpost."); // gaurded by 5 scouts, 2 adv
    lv3missions.put(32, "Steal enemy supplies."); // gaurded by 3 adv

    lv4missions.put(41, "Destroy 15 enemy ships."); // 10 scouts, 5 adv
    lv4missions.put(42, "Destroy enemy weaponry."); // weapon, 5 adv
    lv4missions.put(43, "Resolve skirmish."); // 5 friendlies vs 10 adv

    lv5missions.put(50, "Destroy the enemy spacestation."); // 20 scouts, 10 adv, 1 captain

    currentLevel = 1;
  }

  public void getCurrentMissionText() {

  }

  public void getCurrentMissionId() {

  }

  public void nextMission() {

  }
}
*/
public class MyShip extends SpaceShip {

  private double maxHealth,currentHealth,currentFuel,maxFuel,currentHeat,maxHeat,currentSpeed,maxSpeed;

  public MyShip() {
    corners = 11;
    int[] xC = {-14,-10,-12,2,4,14,4,2,-12,-10,-14};
    int[] yC = {-4,-4,-12,-12,-2,0,2,12,12,4,4};
    xCorners = xC;
    yCorners = yC;
    fillColor = color(77,77,255);
    strokeColor = color(255,255,255);
    myCenterX = 400;
    myCenterY = 400;
    myDirectionX = 0;
    myDirectionY = 0;
    myPointDirection = 0;
    maxHealth = 10; //10
    currentHealth = 10; //10
    maxFuel = 100; //100
    currentFuel = 100; //100
    maxHeat = 50; //50
    currentHeat = 0;
    MAX_VELOCITY = 5;
    currentSpeed = 0;
    maxSpeed = 0;
  }
  public double getMaxHealth(){return maxHealth;}
  public double getCurrentHealth(){return currentHealth;}
  public void setMaxHealth(double health){maxHealth = health;}
  public void setCurrentHealth(double health){
    currentHealth += health;
    if(currentHealth > maxHealth) {
      currentHealth = maxHealth;
    }
  }
  public double getMaxFuel(){return maxFuel;}
  public double getCurrentFuel(){return currentFuel;}
  public void setMaxFuel(double mf){maxFuel = mf;}
  public void setCurrentFuel(double cf){
    currentFuel += cf;
    if(currentFuel > maxFuel) {
      currentFuel = maxFuel;
    }
  }
  public double getMaxHeat(){return maxHeat;}
  public double getCurrentHeat(){return currentHeat;}
  public void setMaxHeat(double mh){maxHeat = mh;}
  public void setCurrentHeat(double ch){
    currentHeat += ch;
    if(currentHeat < 0) {
      currentHeat = 0;
    }
  }
  public void hyperspace() {
    if(currentFuel > 10) {
      myCenterX = (int)(Math.random()*MAP_WIDTH);
      myCenterY = (int)(Math.random()*MAP_HEIGHT);
      myDirectionX = 0;
      myDirectionY = 0;
      myPointDirection = (int)(Math.random()*360);
      currentFuel -= 10;
    }
  }
  public void teleport(double x, double y) {
    if(currentFuel > 10) {
      myCenterX = x;
      myCenterY = y;
      myDirectionX = 0;
      myDirectionY = 0;
      currentFuel -= 10;
    }
  }
  public double getCurrentSpeed() {
    currentSpeed = Math.abs(Math.sqrt(Math.pow(myDirectionX,2) + Math.pow(myDirectionY,2)));
    return currentSpeed;
  }
  public double getMaxSpeed() {
    maxSpeed = Math.sqrt(Math.pow(MAX_VELOCITY,2) + Math.pow(MAX_VELOCITY,2));
    return maxSpeed;
  }

}
public class Particle extends Floater {
  public double initX, initY;
  public Particle(double x, double y, int c) {
    corners = 3;
    strokeColor = c;
    fillColor = color(0,0,0);
    int[] xC = {0,5,-5};
    int[] yC = {5,-5,-5};
    xCorners = xC;
    yCorners = yC;
    myCenterX = x;
    myCenterY = y;
    initX = x;
    initY = y;
    myDirectionX = Math.random()*4-2;
    myDirectionY = Math.random()*4-2;
    myPointDirection = Math.random()*360;
  }
  public void setX(int x){}
  public int getX(){return (int)myCenterX;}
  public void setY(int y){}
  public int getY(){return (int)myCenterY;}
  public void setDirectionX(double x){}
  public double getDirectionX(){return myDirectionX;}
  public void setDirectionY(double y){}
  public double getDirectionY(){return myDirectionY;}
  public void setPointDirection(int degrees){}
  public double getPointDirection(){return myPointDirection;}
}
public abstract class SpaceShip extends Floater {

  protected double MAX_VELOCITY;
  protected final static double SHIP_RECOIL = -0.001f;

  public void setX(int x){myCenterX = x;}
  public int getX(){return (int)myCenterX;}
  public void setY(int y){myCenterY = y;}
  public int getY(){return (int)myCenterY;}
  public void setDirectionX(double x){myDirectionX = x;}
  public double getDirectionX(){return myDirectionX;}
  public void setDirectionY(double y){myDirectionY = y;}
  public double getDirectionY(){return myDirectionY;}
  public void setPointDirection(int degrees){myPointDirection = degrees;}
  public double getPointDirection(){return myPointDirection;}

  public void move() {
    if(myDirectionX > MAX_VELOCITY) {
      myDirectionX = MAX_VELOCITY;
    }
    if(myDirectionX < -(MAX_VELOCITY)) {
      myDirectionX = -(MAX_VELOCITY);
    }
    if(myDirectionY > MAX_VELOCITY) {
      myDirectionY = MAX_VELOCITY;
    }
    if(myDirectionY < -(MAX_VELOCITY)) {
      myDirectionY = -(MAX_VELOCITY);
    }
    myCenterX += myDirectionX;
    myCenterY += myDirectionY;
  }

  public void recoil() {
    accelerate(SHIP_RECOIL);
  }
}
public class Spacestation {
  public float x,y,radius;
  private float diameter;
  private double currentHealth, maxHealth;
  private String type;
  private int fillColor, strokeColor;

  public Spacestation(String t) {
    this.currentHealth = 10;
    this.maxHealth = 10;
    this.type = t;
    this.diameter = 500;
    this.radius = this.diameter/2;

    if(type == "friendly") {
      this.x = 400;
      this.y = 400;
      this.strokeColor = color(0,0,255);
    } else if (type == "enemy") {
      this.x = 4600;
      this.y = 4600;
      this.strokeColor = color(255,0,0);
    }
  }

  public void show() {
    fill(0,0,0,0);
    stroke(this.strokeColor);
    strokeWeight(20);
    ellipse(this.x, this.y, this.diameter, this.diameter);
  }

  public double getCurrentHealth() {
    return this.currentHealth;
  }

  public void setCurrentHealth(double ch) {
    this.currentHealth += ch;
  }

  public double getMaxHealth() {
    return this.maxHealth;
  }

  public void setMaxHealth(double mh) {
    maxHealth += mh;
  }
}
public class Star {
  double x,y;
  Star() {
    x = (double)(Math.random()*MAP_WIDTH);
    y = (double)(Math.random()*MAP_HEIGHT);
  }
  public void show() {
    fill(255);
    stroke(255);
    ellipse((float)x,(float)y,1,1);
  }
}
public class WingShip extends SpaceShip {
  private double currentHealth,maxHealth,ACCELERATION,deltaX,deltaY,angle;
  private boolean inRange;
  private ArrayList<EnemyShip> temp = new ArrayList<EnemyShip>();

  public WingShip() {
    corners = 11;
    int[] xC = {-14,-10,-12,2,4,14,4,2,-12,-10,-14};
    int[] yC = {-4,-4,-12,-12,-2,0,2,12,12,4,4};
    xCorners = xC;
    yCorners = yC;
    fillColor = color(0,191,255);
    strokeColor = color(255,255,255);
    myCenterX = random(150,650);
    myCenterY = random(150,650);
    myDirectionX = 0;
    myDirectionY = 0;
    myPointDirection = 0;
    this.currentHealth = 5;
    this.maxHealth = 5;
    ACCELERATION = random(0.05f,0.3f);
    MAX_VELOCITY = random(2.0f,4.0f);
    inRange = false;
  }
  public double getMaxHealth(){return maxHealth;}
  public double getCurrentHealth(){return currentHealth;}
  public void setMaxHealth(double health){maxHealth = health;}
  public void setCurrentHealth(double health){
    currentHealth += health;
    if(currentHealth > maxHealth) {
      currentHealth = maxHealth;
    }
  }

  public void move() {

    /* Copies enemyships arraylist to temp arraylist */
    temp.clear();
    for(int e = 0; e < enemyShips.size(); e++) {
      temp.add(enemyShips.get(e));
    }

    if (temp.size() > 0) {
      while(temp.size() > 1) { /* While loop that removes farthest ship */
        if (dist((float)this.myCenterX,(float)this.myCenterY,temp.get(0).getX(),temp.get(0).getY()) > dist((float)this.myCenterX,(float)this.myCenterY,temp.get(1).getX(),temp.get(1).getY())) {
          temp.remove(0);
        } else {
          temp.remove(1);
        }
      } /* In the end, we have an arraylist with size one with the ship closest */

      deltaX = temp.get(0).getX() - myCenterX;
      deltaY = temp.get(0).getY() - myCenterY;

      angle = atan((float)(deltaY/deltaX)) * (180/Math.PI);

      if(temp.get(0).getX() < myCenterX) {
        angle += 180;
      }

      myPointDirection = angle;
      inRange = dist((float)myCenterX,(float)myCenterY,temp.get(0).getX(),temp.get(0).getY()) <= 1000;

    } else { /* Go towards your ship */

      deltaX = myShip.getX() - myCenterX;
      deltaY = myShip.getY() - myCenterY;

      angle = atan((float)(deltaY/deltaX)) * (180/Math.PI);

      if(myShip.getX() < myCenterX) {
        angle += 180;
      }

      myPointDirection = angle;
      inRange = false;
    }

    /* Accelerate in point direction */
    accelerate(ACCELERATION);

    /* Limit velocity */
    if(myDirectionX > MAX_VELOCITY) {
      myDirectionX = MAX_VELOCITY;
    }
    if(myDirectionX < -(MAX_VELOCITY)) {
      myDirectionX = -(MAX_VELOCITY);
    }
    if(myDirectionY > MAX_VELOCITY) {
      myDirectionY = MAX_VELOCITY;
    }
    if(myDirectionY < -(MAX_VELOCITY)) {
      myDirectionY = -(MAX_VELOCITY);
    }

    /* Actually move it */
    myCenterX += myDirectionX;
    myCenterY += myDirectionY;
  }

  public boolean isInRange() {
    return inRange;
  }
}
  public void settings() {  size(displayWidth,displayHeight,P2D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "AsteroidsGame" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
