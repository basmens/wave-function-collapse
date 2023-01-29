package nl.basmens.wfc;

public class IntModule {
  public final int keyUp;
  public final int keyRight;
  public final int keyDown;
  public final int keyLeft;

  public final Module parent;

  public IntModule(int keyUp, int keyRight, int keyDown, int keyLeft, Module parent) {
    this.keyUp = keyUp;
    this.keyRight = keyRight;
    this.keyDown = keyDown;
    this.keyLeft = keyLeft;

    this.parent = parent;
  }
}
