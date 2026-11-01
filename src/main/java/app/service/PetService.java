package app.service;

import app.domain.AppointmentStatus;
import app.domain.Pet;
import app.http.HttpError;
import app.store.InMemoryStore;

import java.util.List;

public class PetService {
    private final InMemoryStore store;
    public PetService(InMemoryStore store) {
        this.store = store;
    }

    public List<Pet> list(String type, Boolean vaccinated) {
        List<Pet> all = store.findAllPets();
        return all.stream()
                .filter(p -> type == null || p.getType().name().equalsIgnoreCase(type))
                .filter(p -> vaccinated == null || p.isVaccinated() == vaccinated)
                .collect(java.util.stream.Collectors.toList());
    }

    public Pet get(String id) {
        return store.findPetById(id)
                .orElseThrow(() -> HttpError.notFound("Pet not found: " + id));
    }

    public Pet create(Pet pet) {
        validate(pet);
        pet.setId(null);
        return store.savePet(pet);
    }

    public Pet update(String id, Pet updated) {
        Pet existing = get(id);
        validate(updated);

        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setOwnerName(updated.getOwnerName());
        existing.setVaccinated(updated.isVaccinated());

        return store.savePet(existing);
    }

    public void delete(String id) {
        get(id);
        boolean hasAppointments = store.findAllAppointments().stream()
                .anyMatch(a -> id.equals(a.getPetId()) && a.getStatus() == AppointmentStatus.SCHEDULED);
        if (hasAppointments) {
            throw HttpError.conflict("Cannot delete pet with scheduled appointments");
        }
        store.deletePet(id);
    }

    private void validate(Pet pet) {
        if (pet.getName() == null || pet.getName().isBlank()) {
            throw HttpError.badRequest("Name must not be empty");
        }
        if (pet.getType() == null) {
            throw HttpError.badRequest("Type must be CAT or DOG");
        }
        if (pet.getOwnerName() == null || pet.getOwnerName().isBlank()) {
            throw HttpError.badRequest("Owner name must not be empty");
        }
    }
}