package app.service;

import app.domain.Appointment;
import app.domain.AppointmentStatus;
import app.domain.Pet;
import app.domain.PetType;
import app.exception.BadRequestException;
import app.exception.ConflictException;
import app.exception.NotFoundException;
import app.repository.AppointmentRepository;
import app.repository.InMemoryAppointmentRepository;
import app.repository.InMemoryPetRepository;
import app.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests for PetService (POJO without Spring Context)")
class PetServiceTest {

    private AppointmentRepository appointmentRepository;
    private PetService petService;

    @BeforeEach
    void setUp() {
        PetRepository petRepository = new InMemoryPetRepository();
        appointmentRepository = new InMemoryAppointmentRepository();
        petService = new PetService(petRepository, appointmentRepository);
    }

    @Test
    @DisplayName("create() should successfully save pet and assign an ID")
    void shouldCreatePetSuccessfully() {
        Pet pet = new Pet(null, "Barsik", PetType.CAT, "Ivan", false);
        Pet created = petService.create(pet);

        assertNotNull(created.getId());
        assertEquals("Barsik", created.getName());
        assertEquals(PetType.CAT, created.getType());
        assertEquals("Ivan", created.getOwnerName());
        assertFalse(created.isVaccinated());

        Pet fromRepo = petService.get(created.getId());
        assertEquals("Barsik", fromRepo.getName());
    }

    @Test
    @DisplayName("create() should throw BadRequestException when pet is null")
    void shouldThrowWhenPetIsNull() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> petService.create(null));
        assertEquals("Pet data must not be null", ex.getMessage());
    }

    @Test
    @DisplayName("create() should throw BadRequestException when name is blank or null")
    void shouldThrowWhenNameIsBlank() {
        Pet nullName = new Pet(null, null, PetType.CAT, "Ivan", false);
        assertThrows(BadRequestException.class, () -> petService.create(nullName));

        Pet emptyName = new Pet(null, "   ", PetType.CAT, "Ivan", false);
        assertThrows(BadRequestException.class, () -> petService.create(emptyName));
    }

    @Test
    @DisplayName("create() should throw BadRequestException when type is null")
    void shouldThrowWhenTypeIsNull() {
        Pet noType = new Pet(null, "Barsik", null, "Ivan", false);
        assertThrows(BadRequestException.class, () -> petService.create(noType));
    }

    @Test
    @DisplayName("create() should throw BadRequestException when ownerName is blank or null")
    void shouldThrowWhenOwnerNameIsBlank() {
        Pet nullOwner = new Pet(null, "Barsik", PetType.CAT, null, false);
        assertThrows(BadRequestException.class, () -> petService.create(nullOwner));

        Pet blankOwner = new Pet(null, "Barsik", PetType.CAT, "   ", false);
        assertThrows(BadRequestException.class, () -> petService.create(blankOwner));
    }

    @Test
    @DisplayName("get() should return pet when exists and throw NotFoundException when absent")
    void shouldGetPetOrThrowNotFound() {
        Pet created = petService.create(new Pet(null, "Rex", PetType.DOG, "Oleg", true));
        Pet found = petService.get(created.getId());
        assertEquals("Rex", found.getName());

        assertThrows(NotFoundException.class, () -> petService.get("9999"));
    }

    @Test
    @DisplayName("list() should filter pets by type, vaccinated or return all")
    void shouldListPetsWithFilters() {
        petService.create(new Pet(null, "Barsik", PetType.CAT, "Ivan", false));
        petService.create(new Pet(null, "Murka", PetType.CAT, "Anna", true));
        petService.create(new Pet(null, "Rex", PetType.DOG, "Oleg", true));
        petService.create(new Pet(null, "Bobik", PetType.DOG, "Petr", false));

        assertEquals(4, petService.list(null, null).size());

        List<Pet> cats = petService.list("CAT", null);
        assertEquals(2, cats.size());
        assertTrue(cats.stream().allMatch(p -> p.getType() == PetType.CAT));

        List<Pet> vaccinated = petService.list(null, true);
        assertEquals(2, vaccinated.size());
        assertTrue(vaccinated.stream().allMatch(Pet::isVaccinated));

        List<Pet> vaccinatedDogs = petService.list("dog", true);
        assertEquals(1, vaccinatedDogs.size());
        assertEquals("Rex", vaccinatedDogs.getFirst().getName());
    }

    @Test
    @DisplayName("update() should update existing pet fields")
    void shouldUpdateExistingPet() {
        Pet created = petService.create(new Pet(null, "Rex", PetType.DOG, "Oleg", false));

        Pet updatedData = new Pet(null, "Rex Senior", PetType.DOG, "Oleg", true);
        Pet updated = petService.update(created.getId(), updatedData);

        assertEquals(created.getId(), updated.getId());
        assertEquals("Rex Senior", updated.getName());
        assertTrue(updated.isVaccinated());

        Pet reloaded = petService.get(created.getId());
        assertEquals("Rex Senior", reloaded.getName());
        assertTrue(reloaded.isVaccinated());
    }

    @Test
    @DisplayName("update() should throw NotFoundException when pet does not exist")
    void shouldThrowNotFoundOnUpdateMissingPet() {
        Pet updateData = new Pet(null, "Ghost", PetType.CAT, "Unknown", false);
        assertThrows(NotFoundException.class, () -> petService.update("404", updateData));
    }

    @Test
    @DisplayName("update() should prevent bypassing dog vaccination rule when dog has scheduled appointments")
    void shouldPreventUnvaccinatingDogWithScheduledAppointments() {
        Pet dog = petService.create(new Pet(null, "Rex", PetType.DOG, "Oleg", true));

        Appointment scheduled = new Appointment(null, dog.getId(), "Dr. House",
                LocalDateTime.now().plusDays(1), 30, AppointmentStatus.SCHEDULED);
        appointmentRepository.save(scheduled);

        Pet attemptToUnvaccinate = new Pet(null, "Rex", PetType.DOG, "Oleg", false);
        ConflictException ex = assertThrows(ConflictException.class,
                () -> petService.update(dog.getId(), attemptToUnvaccinate));
        assertEquals("Dog with scheduled appointments must remain vaccinated", ex.getMessage());

        Pet attemptToChangeToUnvaccinatedCatToDog = petService.create(new Pet(null, "Tom", PetType.CAT, "Anna", false));
        Appointment catAppointment = new Appointment(null, attemptToChangeToUnvaccinatedCatToDog.getId(), "Dr. House",
                LocalDateTime.now().plusDays(2), 30, AppointmentStatus.SCHEDULED);
        appointmentRepository.save(catAppointment);

        Pet changeCatToUnvaccinatedDog = new Pet(null, "Tom", PetType.DOG, "Anna", false);
        assertThrows(ConflictException.class,
                () -> petService.update(attemptToChangeToUnvaccinatedCatToDog.getId(), changeCatToUnvaccinatedDog));
    }

    @Test
    @DisplayName("update() should allow updating pet without scheduled appointments even to unvaccinated dog")
    void shouldAllowUpdatingPetWithoutScheduledAppointments() {
        Pet dog = petService.create(new Pet(null, "Rex", PetType.DOG, "Oleg", true));

        Pet unvaccinate = new Pet(null, "Rex", PetType.DOG, "Oleg", false);
        Pet result = petService.update(dog.getId(), unvaccinate);
        assertFalse(result.isVaccinated());
    }

    @Test
    @DisplayName("delete() should remove pet when there are no appointments")
    void shouldDeletePetSuccessfully() {
        Pet created = petService.create(new Pet(null, "Barsik", PetType.CAT, "Ivan", false));
        String petId = created.getId();

        petService.delete(petId);

        assertThrows(NotFoundException.class, () -> petService.get(petId));
    }

    @Test
    @DisplayName("delete() should throw NotFoundException if pet does not exist")
    void shouldThrowNotFoundOnDeleteMissingPet() {
        assertThrows(NotFoundException.class, () -> petService.delete("missing-id"));
    }

    @Test
    @DisplayName("delete() should throw ConflictException if pet has SCHEDULED appointments")
    void shouldThrowConflictWhenDeletingPetWithScheduledAppointments() {
        Pet pet = petService.create(new Pet(null, "Rex", PetType.DOG, "Oleg", true));

        Appointment scheduled = new Appointment(null, pet.getId(), "Dr. House",
                LocalDateTime.now().plusDays(1), 30, AppointmentStatus.SCHEDULED);
        appointmentRepository.save(scheduled);

        ConflictException ex = assertThrows(ConflictException.class, () -> petService.delete(pet.getId()));
        assertEquals("Cannot delete pet with scheduled appointments", ex.getMessage());

        assertNotNull(petService.get(pet.getId()));
    }

    @Test
    @DisplayName("delete() should cascade delete historical (COMPLETED/CANCELLED) appointments to avoid orphan records")
    void shouldCascadeDeleteHistoricalAppointmentsWhenPetIsDeleted() {
        Pet pet = petService.create(new Pet(null, "Murka", PetType.CAT, "Anna", true));

        Appointment completed = new Appointment(null, pet.getId(), "Dr. House",
                LocalDateTime.now().minusDays(1), 30, AppointmentStatus.COMPLETED);
        Appointment cancelled = new Appointment(null, pet.getId(), "Dr. House",
                LocalDateTime.now().plusDays(2), 30, AppointmentStatus.CANCELLED);
        Appointment savedCompleted = appointmentRepository.save(completed);
        Appointment savedCancelled = appointmentRepository.save(cancelled);

        petService.delete(pet.getId());

        assertThrows(NotFoundException.class, () -> petService.get(pet.getId()));
        assertTrue(appointmentRepository.findById(savedCompleted.getId()).isEmpty(),
                "Completed appointment should be cascade removed upon pet deletion");
        assertTrue(appointmentRepository.findById(savedCancelled.getId()).isEmpty(),
                "Cancelled appointment should be cascade removed upon pet deletion");
    }
}
