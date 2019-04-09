package net.tarpn.app;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultApplication implements Application<String> {

    private Application<String> delegate;
    private final ApplicationRegistry applicationRegistry;

    public DefaultApplication(ApplicationRegistry applicationRegistry) {
        this.applicationRegistry = applicationRegistry;
    }

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    public void onConnect(Consumer<String> response) {
        response.accept("> Welcome to default. Enter 'help' for help.");
    }

    @Override
    public void handle(String request, Consumer<String> response, Runnable closer) {
        if (delegate != null) {
            delegate.handle(request, response, () -> {
                delegate.onDisconnect(response);
                delegate = null;
            });
        } else {
            // Handle default commands
            String[] tokens = request.split(" ");
            Commands command = Commands.fromString(tokens[0]);

            switch (command) {
                case HELP:
                    response.accept("> Help:");
                    Commands.validCommands().forEach(cmd ->
                        response.accept(" - " + cmd.getName() + " : " + cmd.getHelp()));
                    break;
                case BYE:
                    closer.run();
                    break;
                case APPS:
                    response.accept("> Applications:");
                    applicationRegistry.listApplications().forEach(appName -> response.accept(" - " + appName));
                    break;
                case LOAD:
                    String arg = tokens[1];
                    Application<String> application = applicationRegistry.loadApplication(arg, String.class);
                    if (application instanceof ApplicationRegistry.NoApplication) {
                        response.accept("> Unknown application " + request);
                    } else {
                        if (application.getType().equals(String.class)) {
                            delegate = application;
                            delegate.onConnect(response);
                        } else {
                            // ???
                        }
                    }
                    break;
                case UNKNOWN:
                    response.accept("> Unknown command: " + tokens[0] + ". Try 'help' for help.");
                    break;
            }
        }
    }

    @Override
    public void onDisconnect(Consumer<String> response) {
        if (delegate != null) {
            delegate.onDisconnect(response);
            delegate = null;
        } else {
            response.accept("> Goodbye from default");
        }
    }

    interface Command {
        String getName();

        String getHelp();
    }

    enum Commands implements Command {
        HELP() {
            @Override
            public String getName() {
                return "help";
            }

            @Override
            public String getHelp() {
                return "Print this help.";
            }
        },
        BYE() {
            @Override
            public String getName() {
                return "bye";
            }

            @Override
            public String getHelp() {
                return "Disconnect from the server.";
            }
        },
        APPS() {
            @Override
            public String getName() {
                return "apps";
            }

            @Override
            public String getHelp() {
                return "List available applications.";
            }
        },
        LOAD() {
            @Override
            public String getName() {
                return "load";
            }

            @Override
            public String getHelp() {
                return "Load an application, usage: LOAD something";
            }
        },
        UNKNOWN() {
            @Override
            public String getName() {
                return "";
            }

            @Override
            public String getHelp() {
                return "";
            }
        };

        static Commands fromString(String cmd) {
            for (Commands value : Commands.values()) {
                if (value.getName().equalsIgnoreCase(cmd)) {
                    return value;
                }
            }
            return UNKNOWN;
        }

        static Set<Command> validCommands() {
            return Stream.of(Commands.values())
                    .filter(cmd -> !cmd.equals(Commands.UNKNOWN))
                    .collect(Collectors.toSet());
        }
    }
}
