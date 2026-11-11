package app.repository;

import app.domain.Pet;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryPetRepository implements PetRepository {

    private final Map<String, Pet> pets = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public List<Pet> findAll() {
        return new ArrayList<>(pets.values());
    }

    @Override
    public Optional<Pet> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(pets.get(id));
    }

    @Override
    public Pet save(Pet pet) {
        if (pet.getId() == null) {
            pet.setId(String.valueOf(idCounter.getAndIncrement()));
        }
        pets.put(pet.getId(), pet);
        return pet;
    }

    @Override
    public void deleteById(String id) {
        if (id != null) {
            pets.remove(id);
        }
    }
}
