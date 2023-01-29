package nl.basmens;

import java.util.ArrayList;

import nl.basmens.wfc.IntModule;
import nl.basmens.wfc.KeyPairsMap;
import nl.basmens.wfc.Tile;
import nl.basmens.wfc.Wfc;
import nl.basmens.wfc.WfcFeatures;
import nl.benmens.processing.PApplet;
import processing.core.PImage;
import processing.opengl.PGraphicsOpenGL;
import processing.event.MouseEvent;

public class Main extends PApplet {
  private Wfc wfc;

  // ===================================================================================================================
  // Native processing functions for lifecycle
  // ===================================================================================================================
  @Override
  public void settings() {
    // fullScreen(P2D);
    size(1800, 1200, P2D);
  }

  @Override
  public void setup() {
    // A workaround for noSmooth() not being compatible with P2D
    ((PGraphicsOpenGL) g).textureSampling(3);

    // Load modules
    ArrayList<nl.basmens.wfc.Module> modules = new ArrayList<>();

    String path = Main.class.getResource("/patterns/circuit/").toString().substring(6);
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

    // Assign key pairs
    KeyPairsMap keyPairsMap = new KeyPairsMap();
    keyPairsMap.addPair("board", "board");
    keyPairsMap.addPair("chip", "chip");
    keyPairsMap.addPair("chip edge left", "chip edge right");
    keyPairsMap.addPair("wire", "wire");
    keyPairsMap.addPair("pin", "pin");

    // Create WfcFeatures
    WfcFeatures features = new WfcFeatures(modules.toArray(new nl.basmens.wfc.Module[] {}), keyPairsMap, true);

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

    // Create Wfc
    wfc = new Wfc(15, 10, features, 5);
  }

  @Override
  public void draw() {
    background(0);

    // Draw grid
    stroke(50);
    strokeWeight(6);
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
          fill(160);
          textSize(60);
          textAlign(CENTER, CENTER);
          text(grid[x][y].getPossibilities().size(), (float) ((x + 0.5) * tileW), (float) ((y + 0.5) * tileH));
        }
      }
    }
  }

  // ===================================================================================================================
  // Events
  // ===================================================================================================================
  @Override
  public void mouseClicked(MouseEvent event) {
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
