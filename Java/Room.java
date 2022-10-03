package byow.Core;


public class Room {
    //private int id;
    private int height;
    private int width;
    private  Position basePos;



    Room(int height, int weight, Position basePos) {
        this.height = height;
        this.width = weight;

        this.basePos = basePos;




    }

    public void setHeight(int y) {
        this.height = y;
    }

    public void setWidth(int x) {
        this.width = x;
    }

    public int getHeight() {
        return this.height;
    }
    public int getWidth() {
        return this.width;
    }



    public Position getBasePos() {
        return this.basePos;
    }

}
