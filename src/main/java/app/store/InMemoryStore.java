package app.store;

import app.domain.Appointment;
import app.domain.Pet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryStore {
    private final Map<String, Pet> pets = new ConcurrentHashMap<>();
    private final AtomicLong petIdCounter = new AtomicLong(1);

    public List<Pet> findAllPets() {
        return new ArrayList<>(pets.values());
    }

    public Optional<Pet> findPetById(String id) {
        return Optional.ofNullable(pets.get(id));
    }

    public Pet savePet(Pet pet) {
        if (pet.getId() == null) {
            pet.setId(String.valueOf(petIdCounter.getAndIncrement()));
        }
        // Сохр в карту
        pets.put(pet.getId(), pet);
        return pet;
    }

    public void deletePet(String id) {
        pets.remove(id);
    }

    private final Map<String, Appointment> appointments = new ConcurrentHashMap<>();
    private final AtomicLong appointmentIdCounter = new AtomicLong(1);

    public List<Appointment> findAllAppointments() {
        return new ArrayList<>(appointments.values());
    }

    public Optional<Appointment> findAppointmentById(String id) {
        return Optional.ofNullable(appointments.get(id));
    }

    public Appointment saveAppointment(Appointment appointment) {
        if (appointment.getId() == null) {
            appointment.setId(String.valueOf(appointmentIdCounter.getAndIncrement()));
        }
        appointments.put(appointment.getId(), appointment);
        return appointment;
    }
    public void deleteAppointment(String id) {
        appointments.remove(id);
    }
}