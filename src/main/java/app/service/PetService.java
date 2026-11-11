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

        boolean willBeUnvaccinatedDog = updated.getType().isVaccinationRequired() && !updated.isVaccinated();
        if (willBeUnvaccinatedDog) {
            boolean hasScheduledAppointments = appointmentRepository.findAll().stream()
                    .anyMatch(a -> id.equals(a.getPetId()) && a.getStatus() == AppointmentStatus.SCHEDULED);
            if (hasScheduledAppointments) {
                throw new ConflictException("Dog with scheduled appointments must remain vaccinated");
            }
        }

        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setOwnerName(updated.getOwnerName());
        existing.setVaccinated(updated.isVaccinated());

        return petRepository.save(existing);
    }

    public void delete(String id) {
        get(id);
        boolean hasScheduled = appointmentRepository.findAll().stream()
                .anyMatch(a -> id.equals(a.getPetId()) && a.getStatus() == AppointmentStatus.SCHEDULED);
        if (hasScheduled) {
            throw new ConflictException("Cannot delete pet with scheduled appointments");
        }

        List<Appointment> historical = appointmentRepository.findAll().stream()
                .filter(a -> id.equals(a.getPetId()))
                .toList();
        for (Appointment appointment : historical) {
            appointmentRepository.deleteById(appointment.getId());
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
