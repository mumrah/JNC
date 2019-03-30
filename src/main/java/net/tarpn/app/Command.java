package net.tarpn.app;

import net.tarpn.util.ParsedLine;

public interface Command {
    String getName();

    String getHelp();

    boolean match(ParsedLine parsedLine);
}
