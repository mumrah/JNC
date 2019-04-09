package net.tarpn.app;

import net.tarpn.network.netrom.NetworkPrimitive;

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

}
