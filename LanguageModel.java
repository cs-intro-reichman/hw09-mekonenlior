import java.util.HashMap;
import java.util.Random;

public class LanguageModel {

    // The map of this model.
    // Maps windows to lists of charachter data objects.
    HashMap<String, List> CharDataMap;

    // The window length used in this model.
    int windowLength;

    // The random number generator used by this model.
    private Random randomGenerator;

    /**
     * Constructs a language model with the given window length and a given
     * seed value. Generating texts from this model multiple times with the
     * same seed value will produce the same random texts. Good for debugging.
     */
    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new HashMap<String, List>();
    }

    /**
     * Constructs a language model with the given window length.
     * Generating texts from this model multiple times will produce
     * different random texts. Good for productaion.
     */
    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new HashMap<String, List>();
    }

    /** Builds a language model from the text in the given file (the corpus). */
    public void train(String fileName) {
        String window = "";
        char c;
        In in = new In(fileName);
        // Reads some characters to form the first window
        for (int i = 0; i < windowLength; i++) {
            char tempChar = in.readChar(); // Reads chars
            window += tempChar; // Adds the chars to form the first window
        }
        // Processes the entire text, one character at a time
        while (!in.isEmpty()) {
            // Gets the next character
            c = in.readChar();
            // Checks if the window is already in the map
            List probs = CharDataMap.get(window);
            // If the window was not found in the map
            if (probs == null) {
                // Creates a new empty list, and adds (window,list) to the map
                probs = new List();
                CharDataMap.put(window, probs);
            }
            // Calculates the counts of the current character.
            probs.update(c);
            // Advances the window: adds c to the window’s end, and deletes the
            // window's first character.
            window = (window + c).substring(1);
        }
        // The entire file has been processed, and all the characters have been counted.
        // Proceeds to compute and set the p and cp fields of all the CharData objects
        // in each linked list in the map.
        for (List probs : CharDataMap.values()) {
            calculateProbabilities(probs);
        }
    }

    // Computes and sets the probabilities (p and cp fields) of all the
    // characters in the given list. */
    public void calculateProbabilities(List probs) {
        int totalChars = 0;
        for (int i = 0; i < probs.getSize(); i++) {
            totalChars += probs.listIterator(i).current.cp.count;
        }
        // For each character in the given list, this loop computes and set it's
        // probabilities (p and cp fields)
        for (int i = 0; i < probs.getSize(); i++) {
            CharData currentChar = probs.listIterator(i).current.cp;
            CharData previousChar = (i == 0) ? null : probs.listIterator(i - 1).current.cp;
            currentChar.p = (double) currentChar.count / totalChars;
            if (i == 0) {
                currentChar.cp = currentChar.p;
            } else {
                currentChar.cp = currentChar.p + previousChar.cp;
            }
        }
    }

    // Returns a random character from the given probabilities list.
    public char getRandomChar(List probs) {
        double r = randomGenerator.nextDouble();
        int i = 0;
        // Iterates the list, reading the cumulative probabilities as we go along.
        while ((probs.listIterator(i).current.cp.cp < r)) {
            i++;
        } 
        /* Stops at the element whose cumulative probability is greater than
        r, and returns the character of this element */
        return probs.get(i).chr;
    }

    /**
     * Generates a random text, based on the probabilities that were learned during
     * training.
     * 
     * @param initialText     - text to start with. If initialText's last substring
     *                        of size numberOfLetters
     *                        doesn't appear as a key in Map, we generate no text
     *                        and return only the initial text.
     * @param numberOfLetters - the size of text to generate
     * @return the generated text
     */
    public String generate(String initialText, int textLength) {
        if (initialText.length() < windowLength) {
            return initialText;
        }
         /* The text generation process starts by setting the initial window to the last windowLength
           characters of the initial text */
           String window = initialText.substring(initialText.length() - windowLength);
           String generatedText = window;
           /* The text generation process stops when the length of the generated text equals the desired
              text length, as specified by the user.*/
           int numberOfLetters = textLength + windowLength;
           while ((generatedText.length() < numberOfLetters)) {   
               List currentList = CharDataMap.get(window);
               /* In any iteration, if the current window is not found in the map, we stop the process and
                  return the text that was generated so far. */
               if (currentList == null) {
                   break; // Exits the loop.
               }
               generatedText += getRandomChar(currentList);
               /* In each iteration, the window is set to the last windowLength
               characters of the generated text. */ 
               window = generatedText.substring(generatedText.length() - windowLength);
           }
           return generatedText; // Returns the generated text
    }

    /** Returns a string representing the map of this language model. */
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String key : CharDataMap.keySet()) {
            List keyProbs = CharDataMap.get(key);
            str.append(key + " : " + keyProbs + "\n");
        }
        return str.toString();
    }

    public static void main(String[] args) {
        int windowLength = Integer.parseInt(args[0]);
        String initialText = args[1];
        int generatedTextLength = Integer.parseInt(args[2]);
        Boolean randomGeneration = args[3].equals("random");
        String fileName = args[4];
        // Create the LanguageModel object
        LanguageModel lm;
        if (randomGeneration) {
            lm = new LanguageModel(windowLength); 
        } else {
            lm = new LanguageModel(windowLength, 20);
        }
        // Trains the model, creating the map.
        lm.train(fileName);
        // Generates text, and prints it.
        System.out.println(lm.generate(initialText, generatedTextLength));
    }
}
