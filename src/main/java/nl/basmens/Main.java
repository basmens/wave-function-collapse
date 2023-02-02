package nl.basmens;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;

import nl.basmens.wfc.IntModule;
import nl.basmens.wfc.KeyPairsMap;
import nl.basmens.wfc.Tile;
import nl.basmens.wfc.Wfc;
import nl.basmens.wfc.WfcFeatures;
import nl.benmens.processing.PApplet;
import processing.core.PImage;
import processing.opengl.PGraphicsOpenGL;

public class Main extends PApplet {
  private WfcFeatures features;
  private Wfc wfc;

  // ===================================================================================================================
  // Native processing functions for lifecycle
  // ===================================================================================================================
  @Override
  public void settings() {
    size(3200, 1600, P2D);  // FullScreen
    // size(1800, 1200, P2D);
  }

  @Override
  public void setup() {
    // A workaround for noSmooth() not being compatible with P2D
    ((PGraphicsOpenGL) g).textureSampling(3);
    surface.setLocation(0, 0);

    // Load modules
    // Reference images:
    // https://raw.githubusercontent.com/mxgmn/WaveFunctionCollapse/master/images/circuit-1.png
    // https://raw.githubusercontent.com/mxgmn/WaveFunctionCollapse/master/images/circuit-2.png
    ArrayList<nl.basmens.wfc.Module> modules = new ArrayList<>();

    try {
      URL resource = Main.class.getResource("/patterns/circuit3");
      String path = Paths.get(resource.toURI()).toAbsolutePath().toString() + FileSystems.getDefault().getSeparator();

      PImage image = loadImage(path + "board.png");
      modules.add(new nl.basmens.wfc.Module("board", "board", "board", "board",
          new int[] { 0 }, image));
  
      image = loadImage(path + "chip_centre.png");
      modules.add(new nl.basmens.wfc.Module("chip", "chip", "chip", "chip",
          new int[] { 0 }, image));
  
      image = loadImage(path + "chip_corner.png");
      modules.add(new nl.basmens.wfc.Module("chip edge left", "board", "board", "chip edge right",
          new int[] { 0, 1, 2, 3 }, image));
  
      image = loadImage(path + "chip_edge.png");
      modules.add(new nl.basmens.wfc.Module("chip edge left", "wire", "chip edge right", "chip",
          new int[] { 0, 1, 2, 3 }, image));
  
      image = loadImage(path + "crossover.png");
      modules.add(new nl.basmens.wfc.Module("pin", "wire", "pin", "wire",
          new int[] { 0, 1 }, image));
  
      image = loadImage(path + "hub1.png");
      modules.add(new nl.basmens.wfc.Module("board", "wire", "board", "board",
          new int[] { 0, 1, 2, 3 }, image));
  
      image = loadImage(path + "hub2.png");
      modules.add(new nl.basmens.wfc.Module("board", "wire", "board", "wire",
          new int[] { 0, 1 }, image));
  
      image = loadImage(path + "pin.png");
      modules.add(new nl.basmens.wfc.Module("board", "pin", "board", "pin",
          new int[] { 0, 1 }, image));
  
      image = loadImage(path + "wire_diagonal1.png");
      modules.add(new nl.basmens.wfc.Module("wire", "wire", "board", "board",
          new int[] { 0, 1, 2, 3 }, image));
  
      image = loadImage(path + "wire_diagonal2.png");
      modules.add(new nl.basmens.wfc.Module("wire", "wire", "wire", "wire",
          new int[] { 0, 1 }, image));
  
      image = loadImage(path + "wire_pin.png");
      modules.add(new nl.basmens.wfc.Module("pin", "board", "wire", "board",
          new int[] { 0, 1, 2, 3 }, image));
  
      image = loadImage(path + "wire_straight.png");
      modules.add(new nl.basmens.wfc.Module("board", "wire", "board", "wire",
          new int[] { 0, 1 }, image));
  
      image = loadImage(path + "wire_t.png");
      modules.add(new nl.basmens.wfc.Module("wire", "wire", "board", "wire",
          new int[] { 0, 1, 2, 3 }, image));
  
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }


    // Assign key pairs
    KeyPairsMap keyPairsMap = new KeyPairsMap();
    keyPairsMap.addPair("board", "board");
    keyPairsMap.addPair("chip", "chip");
    keyPairsMap.addPair("chip edge left", "chip edge right");
    keyPairsMap.addPair("wire", "wire");
    keyPairsMap.addPair("pin", "pin");

    // Create WfcFeatures
    features = new WfcFeatures(modules.toArray(nl.basmens.wfc.Module[]::new), keyPairsMap, true);

    int i = 0;
    StringBuilder top = new StringBuilder();
    StringBuilder middle = new StringBuilder();
    StringBuilder bottom = new StringBuilder();
    for (IntModule m : features.getIntModules()) {
      i++;
      if (i > 15) {
        println();
        println(top.toString());
        println(middle.toString());
        println(bottom.toString());

        top = new StringBuilder();
        middle = new StringBuilder();
        bottom = new StringBuilder();

        i = 1;
      }

      top.append("= " + m.keyUp + " =      ");
      middle.append(m.keyLeft + "   " + m.keyRight + "      ");
      bottom.append("= " + m.keyDown + " =      ");
    }
    println();
    println(top.toString());
    println(middle.toString());
    println(bottom.toString());

    startWfc();
  }

  private int startTime;
  private boolean started = false;
  private ArrayList<Integer> times = new ArrayList<>();
  private int threadCount = 1;
  private void startWfc() {
    startTime = millis();
    started = true;

    wfc = new Wfc(100, 50, features, threadCount);
    wfc.start();
  }


  @Override
  public void draw() {
    background(0);

    if (!wfc.isRunning() && started) {
      int timeElapsed = millis() - startTime;
      times.add(timeElapsed);
      started = false;

      if (times.size() == 1000) {
        double average = times.stream().mapToInt(x -> x).sum() / (double) times.size();
        // PApplet.println(timeElapsed + "  -  " + String.format(Locale.ENGLISH, "%.2f", average) + "  -  " + times.size());
        PApplet.println(threadCount + ":  " + String.format(Locale.ENGLISH, "%.3f", average));

        times = new ArrayList<>();
        threadCount++;
      }

      startWfc();
    }

    // Draw grid
    stroke(50);
    strokeWeight(4);
    double tileW = (double) width / wfc.getGridW();
    for (int x = 1; x < wfc.getGridW(); x++) {
      line((float) (x * tileW), 0, (float) (x * tileW), height);
    }
    double tileH = (double) height / wfc.getGridH();
    for (int y = 1; y < wfc.getGridH(); y++) {
      line(0, (float) (y * tileH), width, (float) (y * tileH));
    }

    // Draw tiles
    imageMode(CENTER);
    Tile[][] grid = wfc.getGrid();
    for (int x = 0; x < wfc.getGridW(); x++) {
      for (int y = 0; y < wfc.getGridH(); y++) {
        if (grid[x][y].isCollapsed() && grid[x][y].getModule().parent.getChildItem() instanceof PImage img) {
          pushMatrix();
          translate((float) ((x + 0.5) * tileW), (float) ((y + 0.5) * tileH));
          rotate(HALF_PI * grid[x][y].getModule().parent.rotations[0]);
          image(img, 0, 0, (float) tileW, (float) tileH);
          popMatrix();
        } else {
          fill(180);
          textSize(30);
          textAlign(CENTER, CENTER);
          text(grid[x][y].getPossibilities().size(), (float) ((x + 0.5) * tileW), (float) ((y + 0.5) * tileH));
        }
      }
    }

    fill(0, 150);
    noStroke();
    rect(0, 0, 220, 90);
    fill(255);
    textAlign(LEFT, TOP);
    textSize(60);
    text(times.size(), 20, 8);
  }

  // ===================================================================================================================
  // Events
  // ===================================================================================================================
  @Override
  public void mousePressed() {
    if (mouseButton == LEFT) {
      double tileW = (double) width / wfc.getGridW();
      double tileH = (double) height / wfc.getGridH();
      int x = (int) Math.floor(mouseX / tileW);
      int y = (int) Math.floor(mouseY / tileH);

      wfc.collapseTile(x, y);
    } else {
      startWfc();
      // println(wfc.isRunning());
    }
  }

  @Override
  public void keyPressed() {
    // save("C:/Users/basme/Downloads/wfc result.png");
    times = new ArrayList<>();
    threadCount = 1;
    startWfc();
  }

  // ===================================================================================================================
  // Main function
  // ===================================================================================================================
  public static void main(String[] passedArgs) {
    if (passedArgs != null) {
      PApplet.main(new Object() {
      }.getClass().getEnclosingClass(), passedArgs);
    } else {
      PApplet.main(new Object() {
      }.getClass().getEnclosingClass());
    }
  }
}
