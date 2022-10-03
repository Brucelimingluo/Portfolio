public class OffByN implements CharacterComparator {

    private int value;
    public OffByN(int N) {
        value = N;
    }
    public boolean equalChars(char x, char y) {
        return x - y == value || y - x == value;
    }
}
