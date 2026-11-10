package app.service;

import app.domain.AppointmentStatus;
import app.domain.Pet;
import app.exception.BadRequestException;
import app.exception.ConflictException;
import app.exception.NotFoundException;
import app.repository.AppointmentRepository;
import app.repository.InMemoryAppointmentRepository;
import app.repository.InMemoryPetRepository;
import app.repository.PetRepository;
import app.store.InMemoryStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final AppointmentRepository appointmentRepository;

    public PetService(PetRepository petRepository, AppointmentRepository appointmentRepository) {
        this.petRepository = petRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public PetService(InMemoryStore store) {
        this(new InMemoryPetRepository(), new InMemoryAppointmentRepository());
    }

    public List<Pet> list(String type, Boolean vaccinated) {
        return petRepository.findAll().stream()
                .filter(p -> type == null || p.getType().name().equalsIgnoreCase(type))
                .filter(p -> vaccinated == null || p.isVaccinated() == vaccinated)
                .collect(Collectors.toList());
    }

    public Pet get(String id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pet not found: " + id));
    }

    public Pet create(Pet pet) {
        validate(pet);
        pet.setId(null);
        return petRepository.save(pet);
    }

    public Pet update(String id, Pet updated) {
        Pet existing = get(id);
        validate(updated);

        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setOwnerName(updated.getOwnerName());
        existing.setVaccinated(updated.isVaccinated());

        return petRepository.save(existing);
    }

    public void delete(String id) {
        get(id);
        boolean hasAppointments = appointmentRepository.findAll().stream()
                .anyMatch(a -> id.equals(a.getPetId()) && a.getStatus() == AppointmentStatus.SCHEDULED);
        if (hasAppointments) {
            throw new ConflictException("Cannot delete pet with scheduled appointments");
        }
        petRepository.deleteById(id);
    }

    private void validate(Pet pet) {
        if (pet == null) {
            throw new BadRequestException("Pet data must not be null");
        }
        if (pet.getName() == null || pet.getName().isBlank()) {
            throw new BadRequestException("Name must not be empty");
        }
        if (pet.getType() == null) {
            throw new BadRequestException("Type must be CAT or DOG");
        }
        if (pet.getOwnerName() == null || pet.getOwnerName().isBlank()) {
            throw new BadRequestException("Owner name must not be empty");
        }
    }
}
