package app.repository;

import app.domain.Pet;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPetRepository {

    public static PetRepository create() {
        Map<UUID, Pet> storage = new ConcurrentHashMap<>();

        return (PetRepository) Proxy.newProxyInstance(
                PetRepository.class.getClassLoader(),
                new Class<?>[]{PetRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        Pet pet = (Pet) args[0];
                        if (pet.getId() == null) {
                            pet.setId(UUID.randomUUID());
                        }
                        storage.put(pet.getId(), pet);
                        yield pet;
                    }
                    case "findById" -> {
                        UUID id = (UUID) args[0];
                        yield Optional.ofNullable(storage.get(id));
                    }
                    case "findAll" -> new ArrayList<>(storage.values());
                    case "delete" -> {
                        Pet pet = (Pet) args[0];
                        if (pet != null && pet.getId() != null) {
                            storage.remove(pet.getId());
                        }
                        yield null;
                    }
                    case "deleteById" -> {
                        UUID id = (UUID) args[0];
                        storage.remove(id);
                        yield null;
                    }
                    case "deleteAll" -> {
                        if (args != null && args.length > 0 && args[0] instanceof Iterable<?> iterable) {
                            for (Object item : iterable) {
                                if (item instanceof Pet p && p.getId() != null) {
                                    storage.remove(p.getId());
                                }
                            }
                        } else {
                            storage.clear();
                        }
                        yield null;
                    }
                    case "count" -> (long) storage.size();
                    case "existsById" -> storage.containsKey((UUID) args[0]);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "InMemoryPetRepository@" + Integer.toHexString(System.identityHashCode(proxy));
                    default -> throw new UnsupportedOperationException("Method " + method.getName() + " is not implemented in InMemoryPetRepository");
                }
        );
    }
}
