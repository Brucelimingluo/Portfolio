public class Palindrome {

    public Deque<Character> wordToDeque(String word) {

        Deque<Character> wordToDeque = new ArrayDeque<>();
        for (int i = 0; i < word.length(); i++) {
            wordToDeque.addLast(word.charAt(i));
        }
        return wordToDeque;
    }

    private boolean isPalindrome(Deque<Character> wordDeque) {
        while (wordDeque.size() > 1) {
            return wordDeque.removeFirst() == wordDeque.removeLast() && isPalindrome(wordDeque);
        }
        return true;
    }
    public boolean isPalindrome(String word) {
        if (word == null) {
            return false;
        } else {
            return isPalindrome(wordToDeque(word));
        }
    }

    private boolean isPalindrome(Deque<Character> wordDeque, CharacterComparator cc) {
        while (wordDeque.size() > 1) {
            return cc.equalChars(wordDeque.removeFirst(), wordDeque.removeLast())
                    && isPalindrome(wordDeque, cc);
        }
        return true;
    }
    public boolean isPalindrome(String word, CharacterComparator cc) {
        if (word == null) {
            return false;
        } else {
            return isPalindrome(wordToDeque(word), cc);
        }
    }

}
