package net.tarpn.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParsedLine {
    private final String line;
    private final String[] tokens;
    private int pos = 0;

    ParsedLine(String line, String[] tokens) {
        this.line = line;
        this.tokens = tokens;
    }

    public static ParsedLine parse(String line) {
        String[] tokens = line.split(" ");
        return new ParsedLine(line, tokens);
    }

    public ParsedWord word() {
        if (pos < tokens.length) {
            return new ParsedWord(tokens[pos++]);
        } else {
            throw new IllegalStateException("Reached the end of the line");
        }
    }

    public ParsedWord word(int idx) {
        return new ParsedWord(tokens[idx]);
    }

    public int size() {
        return tokens.length;
    }

    public String line() {
        return line;
    }

    public List<ParsedWord> words() {
        return Stream.of(tokens).map(ParsedWord::new).collect(Collectors.toList());
    }

    public static class ParsedWord {
        private final String word;

        public ParsedWord(String word) {
            this.word = word;
        }

        public String get() {
            return word;
        }

        public boolean isInt() {
            try {
                Integer.parseInt(word);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        public int asInt() {
            return Integer.parseInt(word);
        }
    }
}
