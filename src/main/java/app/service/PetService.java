package app.service;

import app.domain.Appointment;
import app.domain.AppointmentStatus;
import app.domain.Pet;
import app.exception.BadRequestException;
import app.exception.ConflictException;
import app.exception.NotFoundException;
import app.repository.AppointmentRepository;
import app.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final AppointmentRepository appointmentRepository;

    public PetService(PetRepository petRepository, AppointmentRepository appointmentRepository) {
        this.petRepository = petRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<Pet> list(String type, Boolean vaccinated) {
        return petRepository.findAll().stream()
                .filter(p -> type == null || type.isBlank() || p.getType().name().equalsIgnoreCase(type.trim()))
                .filter(p -> vaccinated == null || p.isVaccinated() == vaccinated)
                .toList();
    }

    public Pet get(String id) {
        return petRepository.findById(parseId(id))
                .orElseThrow(() -> new NotFoundException("Pet not found: " + id));
    }

    @Transactional
    public Pet create(Pet pet) {
        validate(pet);
        pet.setId(null);
        return petRepository.save(pet);
    }

    @Transactional
    public Pet update(String id, Pet updated) {
        Pet existing = get(id);
        validate(updated);

        boolean willBeUnvaccinatedDog = updated.getType().isVaccinationRequired() && !updated.isVaccinated();
        if (willBeUnvaccinatedDog) {
            boolean hasScheduled = appointmentRepository.existsByPet_IdAndStatus(existing.getId(), AppointmentStatus.SCHEDULED);
            if (hasScheduled) {
                throw new ConflictException("Dog with scheduled appointments must remain vaccinated");
            }
        }

        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setOwnerName(updated.getOwnerName());
        existing.setVaccinated(updated.isVaccinated());

        return petRepository.save(existing);
    }

    @Transactional
    public Pet transferOwnership(String id, String newOwnerName) {
        if (newOwnerName == null || newOwnerName.isBlank()) {
            throw new BadRequestException("New owner name must not be empty");
        }

        Pet pet = get(id);
        String oldOwner = pet.getOwnerName();
        pet.setOwnerName(newOwnerName.trim());
        petRepository.save(pet);

        List<Appointment> scheduled = appointmentRepository.findByPet_Id(pet.getId()).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .toList();

        for (Appointment appointment : scheduled) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setNotes("Cancelled due to ownership transfer from " + oldOwner + " to " + newOwnerName.trim());
            appointmentRepository.save(appointment);
        }

        if ("trigger-error".equalsIgnoreCase(newOwnerName.trim())) {
            throw new ConflictException("Simulated error for transaction rollback verification");
        }

        return pet;
    }

    @Transactional
    public void delete(String id) {
        Pet pet = get(id);
        boolean hasScheduled = appointmentRepository.existsByPet_IdAndStatus(pet.getId(), AppointmentStatus.SCHEDULED);
        if (hasScheduled) {
            throw new ConflictException("Cannot delete pet with scheduled appointments");
        }

        List<Appointment> historical = appointmentRepository.findByPet_Id(pet.getId());
        appointmentRepository.deleteAll(historical);

        petRepository.delete(pet);
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

    private UUID parseId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Pet not found: " + id);
        }
    }
}
