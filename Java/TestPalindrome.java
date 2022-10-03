
import org.junit.Test;
import static org.junit.Assert.*;

public class TestPalindrome {
    // You must use this palindrome, and not instantiate
    // new Palindromes, or the autograder might be upset.
    static Palindrome palindrome = new Palindrome();

    @Test
    public void testWordToDeque() {
        Deque d = palindrome.wordToDeque("persiflage");
        String actual = "";
        for (int i = 0; i < "persiflage".length(); i++) {
            actual += d.removeFirst();
        }
        assertEquals("persiflage", actual);
    }
    @Test
    public void testIsPalindrome() {
        assertFalse(palindrome.isPalindrome("cat"));
        assertFalse(palindrome.isPalindrome("cab"));
        assertFalse(palindrome.isPalindrome("aceda"));
        assertFalse(palindrome.isPalindrome("oir"));
        assertFalse(palindrome.isPalindrome("bruce"));
        assertFalse(palindrome.isPalindrome("liming"));
        assertFalse(palindrome.isPalindrome("luo"));
        assertTrue(palindrome.isPalindrome("abccba"));
        assertTrue(palindrome.isPalindrome("noon"));
        assertTrue(palindrome.isPalindrome("a"));
        assertTrue(palindrome.isPalindrome("racecar"));
        assertTrue(palindrome.isPalindrome(""));
        assertFalse(palindrome.isPalindrome(null));
    }

    @Test
    public void testIsPalindromeObo() {
        OffByOne obo = new OffByOne();
        assertTrue(palindrome.isPalindrome("flake", obo));
        assertTrue(palindrome.isPalindrome("", obo));
        assertTrue(palindrome.isPalindrome("hhhgi", obo));
        assertTrue(palindrome.isPalindrome("hihi", obo));
        assertTrue(palindrome.isPalindrome("hjki", obo));
        assertFalse(palindrome.isPalindrome(null, obo));
        assertFalse(palindrome.isPalindrome("abeuca", obo));
        assertFalse(palindrome.isPalindrome("aertd", obo));
    }

}
