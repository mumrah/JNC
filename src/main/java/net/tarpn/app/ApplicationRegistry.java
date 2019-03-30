package net.tarpn.app;

import net.tarpn.datalink.DataLinkManager;
import net.tarpn.datalink.DataLinkSession;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Call;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApplicationRegistry {
    private final Map<String, Application<?>> applications = new HashMap<>();

    public void registerApplication(Application<?> application) {
        applications.put(application.getName(), application);
    }

    public Set<String> listApplications() {
        return applications.keySet();
    }

    @SuppressWarnings("unchecked")
    public <T> Application<T> loadApplication(String name, Class<T> clazz) {
        Application<?> application = applications.getOrDefault(name, NoApplication.noApplication(clazz));
        if (application.getType().equals(clazz)) {
            return (Application<T>) application;
        } else {
            throw new IllegalArgumentException("Application " + name + " does not handle " + clazz);
        }
    }

    public static abstract class NoApplication<T> implements Application<T> {

        public static <T> NoApplication<T> noApplication(Class<T> clazz) {
            return new NoApplication<T>() {
                @Override
                public Class<T> getType() {
                    return clazz;
                }
            };
        }

        @Override
        public String getName() {
            return "none";
        }

        @Override
        public void onConnect(Consumer<T> response) {

        }

        @Override
        public void handle(T request, Consumer<T> response, Runnable closer) {

        }

        @Override
        public void onDisconnect(Consumer<T> response) {

        }
    }

    public static class DefaultApplication implements Application<String> {

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
                        if (application instanceof NoApplication) {
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

    public static class EchoApplication implements Application<String> {
        @Override
        public String getName() {
            return "echo";
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }

        @Override
        public void onConnect(Consumer<String> response) {
            response.accept("Welcome to ECHO");
        }

        @Override
        public void handle(String request, Consumer<String> response, Runnable closer) {
            if (request.equalsIgnoreCase("BYE")) {
                response.accept("Goodbye from ECHO");
                closer.run();
            } else {
                response.accept("You said: " + request);
            }
        }

        @Override
        public void onDisconnect(Consumer<String> response) {
            //response.accept("Goodbye from ECHO");
        }
    }
}
