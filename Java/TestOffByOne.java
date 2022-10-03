import org.junit.Test;
import static org.junit.Assert.*;
public class TestOffByOne {
    // You must use this CharacterComparator and not instantiate
    // new ones, or the autograder might be upset.
    static CharacterComparator offByOne = new OffByOne();
    // Your tests go here.
    @Test
    public void testEqualChars() {
        assertTrue(offByOne.equalChars('a', 'b'));
        assertTrue(offByOne.equalChars('c', 'd'));
        assertTrue(offByOne.equalChars('x', 'y'));
        assertTrue(offByOne.equalChars('F', 'E'));
        assertTrue(offByOne.equalChars('Z', 'Y'));
        assertTrue(offByOne.equalChars('r', 'q'));
        assertFalse(offByOne.equalChars('a', 'c'));
        assertFalse(offByOne.equalChars('z', 'z'));
        assertFalse(offByOne.equalChars('A', 'a'));
        assertFalse(offByOne.equalChars('z', 'p'));
        assertTrue(offByOne.equalChars('A', 'B'));
        assertFalse(offByOne.equalChars('B', 'b'));
        assertFalse(offByOne.equalChars('b', 'C'));
        assertFalse(offByOne.equalChars('%', '@'));
        assertFalse(offByOne.equalChars(')', 'A'));
        assertFalse(offByOne.equalChars(')', 'a'));
        assertFalse(offByOne.equalChars('(', '#'));
        assertFalse(offByOne.equalChars(' ', '@'));
        assertTrue(offByOne.equalChars('&', '%'));
    }

}
