package nl.basmens.wfc;

public class Module {
  public final String keyUp;
  public final String keyRight;
  public final String keyDown;
  public final String keyLeft;

  public final int[] rotations;

  private Object childItem;

  public Module(String keyUp, String keyRight, String keyDown, String keyLeft, int[] rotations, Object childItem) {
    this.keyUp = keyUp;
    this.keyRight = keyRight;
    this.keyDown = keyDown;
    this.keyLeft = keyLeft;

    this.rotations = rotations;

    this.childItem = childItem;
  }

  public Object getChildItem() {
    return childItem;
  }

  public void setImage(Object img) {
    this.childItem = img;
  }
}
