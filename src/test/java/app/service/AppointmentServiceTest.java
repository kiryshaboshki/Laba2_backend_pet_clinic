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

@DisplayName("Unit tests for AppointmentService - Creation and Validation")
class AppointmentServiceTest {

    private AppointmentRepository appointmentRepository;
    private PetRepository petRepository;
    private AppointmentService appointmentService;

    private Pet vaccinatedDog;
    private Pet unvaccinatedDog;
    private Pet unvaccinatedCat;

    @BeforeEach
    void setUp() {
        appointmentRepository = new InMemoryAppointmentRepository();
        petRepository = new InMemoryPetRepository();
        appointmentService = new AppointmentService(appointmentRepository, petRepository);

        vaccinatedDog = petRepository.save(new Pet(null, "Rex", PetType.DOG, "Oleg", true));
        unvaccinatedDog = petRepository.save(new Pet(null, "Bobik", PetType.DOG, "Petr", false));
        unvaccinatedCat = petRepository.save(new Pet(null, "Barsik", PetType.CAT, "Ivan", false));
    }

    @Test
    @DisplayName("create() should successfully schedule appointment for vaccinated dog")
    void shouldCreateAppointmentForVaccinatedDog() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Appointment appointment = new Appointment(null, vaccinatedDog.getId(), "Dr. Aibolit", futureTime, 30, null);

        Appointment created = appointmentService.create(appointment);

        assertNotNull(created.getId());
        assertEquals(vaccinatedDog.getId(), created.getPetId());
        assertEquals("Dr. Aibolit", created.getVetName());
        assertEquals(futureTime, created.getDateTime());
        assertEquals(30, created.getDurationMinutes());
        assertEquals(AppointmentStatus.SCHEDULED, created.getStatus());
    }

    @Test
    @DisplayName("create() should successfully schedule appointment for unvaccinated cat")
    void shouldCreateAppointmentForUnvaccinatedCat() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0);
        Appointment appointment = new Appointment(null, unvaccinatedCat.getId(), "Dr. Aibolit", futureTime, 45, null);

        Appointment created = appointmentService.create(appointment);

        assertNotNull(created.getId());
        assertEquals(AppointmentStatus.SCHEDULED, created.getStatus());
        assertEquals(unvaccinatedCat.getId(), created.getPetId());
    }

    @Test
    @DisplayName("create() should throw ConflictException for unvaccinated dog")
    void shouldThrowConflictForUnvaccinatedDog() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);
        Appointment appointment = new Appointment(null, unvaccinatedDog.getId(), "Dr. Aibolit", futureTime, 30, null);

        ConflictException ex = assertThrows(ConflictException.class, () -> appointmentService.create(appointment));
        assertEquals("Dog must be vaccinated before appointment", ex.getMessage());
    }

    @Test
    @DisplayName("create() should throw NotFoundException when pet does not exist")
    void shouldThrowNotFoundForNonExistingPet() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);
        Appointment appointment = new Appointment(null, "non-existent-pet-id", "Dr. Aibolit", futureTime, 30, null);

        NotFoundException ex = assertThrows(NotFoundException.class, () -> appointmentService.create(appointment));
        assertTrue(ex.getMessage().contains("Pet not found"));
    }

    @Test
    @DisplayName("create() should throw BadRequestException when appointment is null")
    void shouldThrowWhenAppointmentIsNull() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> appointmentService.create(null));
        assertEquals("Appointment data must not be null", ex.getMessage());
    }

    @Test
    @DisplayName("create() should throw BadRequestException when vetName is blank or null")
    void shouldThrowWhenVetNameIsBlank() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        Appointment nullVet = new Appointment(null, vaccinatedDog.getId(), null, futureTime, 30, null);
        assertThrows(BadRequestException.class, () -> appointmentService.create(nullVet));

        Appointment blankVet = new Appointment(null, vaccinatedDog.getId(), "   ", futureTime, 30, null);
        assertThrows(BadRequestException.class, () -> appointmentService.create(blankVet));
    }

    @Test
    @DisplayName("create() should throw BadRequestException when petId is blank or null")
    void shouldThrowWhenPetIdIsBlank() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        Appointment nullPet = new Appointment(null, null, "Dr. Aibolit", futureTime, 30, null);
        assertThrows(BadRequestException.class, () -> appointmentService.create(nullPet));

        Appointment blankPet = new Appointment(null, "   ", "Dr. Aibolit", futureTime, 30, null);
        assertThrows(BadRequestException.class, () -> appointmentService.create(blankPet));
    }

    @Test
    @DisplayName("create() should throw BadRequestException when date is null or in the past")
    void shouldThrowWhenDateTimeIsInPastOrNull() {
        Appointment nullDate = new Appointment(null, vaccinatedDog.getId(), "Dr. Aibolit", null, 30, null);
        assertThrows(BadRequestException.class, () -> appointmentService.create(nullDate));

        LocalDateTime pastTime = LocalDateTime.now().minusHours(2);
        Appointment pastAppointment = new Appointment(null, vaccinatedDog.getId(), "Dr. Aibolit", pastTime, 30, null);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> appointmentService.create(pastAppointment));
        assertEquals("Appointment date must be in the future", ex.getMessage());
    }

    @Test
    @DisplayName("create() should throw BadRequestException when duration is 0 or negative")
    void shouldThrowWhenDurationIsNotPositive() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        Appointment zeroDuration = new Appointment(null, vaccinatedDog.getId(), "Dr. Aibolit", futureTime, 0, null);
        assertThrows(BadRequestException.class, () -> appointmentService.create(zeroDuration));

        Appointment negativeDuration = new Appointment(null, vaccinatedDog.getId(), "Dr. Aibolit", futureTime, -15, null);
        assertThrows(BadRequestException.class, () -> appointmentService.create(negativeDuration));
    }

    @Test
    @DisplayName("get() should return appointment by id or throw NotFoundException")
    void shouldGetAppointmentOrThrowNotFound() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        Appointment created = appointmentService.create(
                new Appointment(null, vaccinatedDog.getId(), "Dr. Aibolit", futureTime, 30, null));

        Appointment found = appointmentService.get(created.getId());
        assertEquals(created.getId(), found.getId());
        assertEquals("Dr. Aibolit", found.getVetName());

        assertThrows(NotFoundException.class, () -> appointmentService.get("missing-id"));
    }

    @Test
    @DisplayName("list() should filter appointments by vetName, status or return all")
    void shouldListAppointmentsWithFilters() {
        LocalDateTime t1 = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime t2 = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);
        LocalDateTime t3 = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);

        Appointment a1 = appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "Dr. House", t1, 30, null));
        Appointment a2 = appointmentService.create(new Appointment(null, unvaccinatedCat.getId(), "Dr. House", t2, 30, null));
        Appointment a3 = appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "Dr. Watson", t3, 30, null));

        assertEquals(3, appointmentService.list(null, null).size());

        List<Appointment> houseAppts = appointmentService.list("dr. house", null);
        assertEquals(2, houseAppts.size());

        List<Appointment> scheduled = appointmentService.list(null, AppointmentStatus.SCHEDULED);
        assertEquals(3, scheduled.size());

        List<Appointment> watsonScheduled = appointmentService.list("Dr. Watson", AppointmentStatus.SCHEDULED);
        assertEquals(1, watsonScheduled.size());
        assertEquals("Dr. Watson", watsonScheduled.get(0).getVetName());
    }

    // -------------------------------------------------------------
    // Interval Overlap and Boundary Conditions Tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("create() should throw ConflictException when booking during existing appointment (Professor's case: 10:00-11:00 and 10:30-11:00)")
    void shouldThrowConflictWhenBookingInsideExistingInterval() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        // Dr. House booked 10:00 - 11:00 (60 min)
        appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 60, null));

        // Attempt to book Dr. House at 10:30 for 30 min (ends at 11:00)
        LocalDateTime overlapTime = baseTime.plusMinutes(30);
        Appointment overlapping = new Appointment(null, unvaccinatedCat.getId(), "Dr. House", overlapTime, 30, null);

        ConflictException ex = assertThrows(ConflictException.class, () -> appointmentService.create(overlapping));
        assertEquals("Vet is already booked at this time", ex.getMessage());
    }

    @Test
    @DisplayName("create() should throw ConflictException when new appointment starts before and ends inside existing")
    void shouldThrowConflictWhenStartingBeforeAndEndingInside() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(12).withMinute(0).withSecond(0).withNano(0);
        // Existing: 12:00 - 13:00 (60 min)
        appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 60, null));

        // New: 11:30 - 12:15 (ends at 12:15, inside 12:00-13:00)
        LocalDateTime earlyStart = baseTime.minusMinutes(30);
        Appointment overlapping = new Appointment(null, unvaccinatedCat.getId(), "Dr. House", earlyStart, 45, null);

        ConflictException ex = assertThrows(ConflictException.class, () -> appointmentService.create(overlapping));
        assertEquals("Vet is already booked at this time", ex.getMessage());
    }

    @Test
    @DisplayName("create() should throw ConflictException when new appointment starts inside and ends after existing")
    void shouldThrowConflictWhenStartingInsideAndEndingAfter() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);
        // Existing: 14:00 - 15:00 (60 min)
        appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 60, null));

        // New: 14:45 - 15:30 (starts at 14:45, inside 14:00-15:00)
        LocalDateTime startInside = baseTime.plusMinutes(45);
        Appointment overlapping = new Appointment(null, unvaccinatedCat.getId(), "Dr. House", startInside, 45, null);

        ConflictException ex = assertThrows(ConflictException.class, () -> appointmentService.create(overlapping));
        assertEquals("Vet is already booked at this time", ex.getMessage());
    }

    @Test
    @DisplayName("create() should throw ConflictException when new appointment completely envelops existing appointment")
    void shouldThrowConflictWhenEnvelopingExistingAppointment() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(16).withMinute(0).withSecond(0).withNano(0);
        // Existing: 16:15 - 16:45 (30 min)
        appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime.plusMinutes(15), 30, null));

        // New: 16:00 - 17:00 (60 min, envelops 16:15-16:45)
        Appointment enveloping = new Appointment(null, unvaccinatedCat.getId(), "Dr. House", baseTime, 60, null);

        ConflictException ex = assertThrows(ConflictException.class, () -> appointmentService.create(enveloping));
        assertEquals("Vet is already booked at this time", ex.getMessage());
    }

    @Test
    @DisplayName("create() should throw ConflictException on 1-minute overlap boundary")
    void shouldThrowConflictOnOneMinuteOverlap() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0);
        // Existing: 18:00 - 19:00 (60 min)
        appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 60, null));

        // New starts at 18:59 for 30 min (overlaps by 1 minute with 18:00-19:00)
        Appointment overlapOneMin = new Appointment(null, unvaccinatedCat.getId(), "Dr. House", baseTime.plusMinutes(59), 30, null);

        ConflictException ex = assertThrows(ConflictException.class, () -> appointmentService.create(overlapOneMin));
        assertEquals("Vet is already booked at this time", ex.getMessage());
    }

    @Test
    @DisplayName("create() should succeed for back-to-back appointments (0-minute gap)")
    void shouldSucceedForBackToBackAppointments() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        // Appointment 1: 10:00 - 10:30 (30 min)
        Appointment a1 = appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 30, null));
        assertNotNull(a1.getId());

        // Appointment 2: 10:30 - 11:00 (exactly at the end of Appointment 1)
        Appointment a2 = appointmentService.create(new Appointment(null, unvaccinatedCat.getId(), "Dr. House", baseTime.plusMinutes(30), 30, null));
        assertNotNull(a2.getId());

        // Appointment 3: 09:30 - 10:00 (ends exactly at the start of Appointment 1)
        Appointment a3 = appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime.minusMinutes(30), 30, null));
        assertNotNull(a3.getId());
    }

    @Test
    @DisplayName("create() should succeed for overlapping time if vet is different")
    void shouldSucceedForDifferentVetsAtSameTime() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
        // Dr. House: 14:00 - 15:00
        Appointment house = appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 60, null));
        assertNotNull(house.getId());

        // Dr. Watson: 14:00 - 15:00 (same time, different doctor)
        Appointment watson = appointmentService.create(new Appointment(null, unvaccinatedCat.getId(), "Dr. Watson", baseTime, 60, null));
        assertNotNull(watson.getId());
    }

    @Test
    @DisplayName("create() should check vetName case-insensitively for overlaps")
    void shouldCheckVetNameCaseInsensitivelyForOverlap() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(3).withHour(16).withMinute(0).withSecond(0).withNano(0);
        // Booked with "dr. dolittle"
        appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "dr. dolittle", baseTime, 45, null));

        // Overlapping booked with "DR. DOLITTLE"
        Appointment overlap = new Appointment(null, unvaccinatedCat.getId(), "DR. DOLITTLE", baseTime.plusMinutes(15), 30, null);

        assertThrows(ConflictException.class, () -> appointmentService.create(overlap));
    }

    // -------------------------------------------------------------
    // Lifecycle Transitions (complete, cancel, delete) and Updates Tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("complete() should change status from SCHEDULED to COMPLETED")
    void shouldCompleteScheduledAppointment() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        Appointment created = appointmentService.create(
                new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 30, null));

        Appointment completed = appointmentService.complete(created.getId());
        assertEquals(AppointmentStatus.COMPLETED, completed.getStatus());

        Appointment reloaded = appointmentService.get(created.getId());
        assertEquals(AppointmentStatus.COMPLETED, reloaded.getStatus());
    }

    @Test
    @DisplayName("complete() should throw ConflictException when appointment is already COMPLETED or CANCELLED")
    void shouldThrowConflictWhenCompletingNonScheduledAppointment() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        Appointment created = appointmentService.create(
                new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 30, null));

        appointmentService.complete(created.getId());

        // Repeat complete
        assertThrows(ConflictException.class, () -> appointmentService.complete(created.getId()));

        // Complete cancelled
        Appointment a2 = appointmentService.create(
                new Appointment(null, unvaccinatedCat.getId(), "Dr. Watson", baseTime, 30, null));
        appointmentService.cancel(a2.getId());

        assertThrows(ConflictException.class, () -> appointmentService.complete(a2.getId()));
    }

    @Test
    @DisplayName("complete() should throw NotFoundException if appointment does not exist")
    void shouldThrowNotFoundWhenCompletingMissingAppointment() {
        assertThrows(NotFoundException.class, () -> appointmentService.complete("non-existent-id"));
    }

    @Test
    @DisplayName("cancel() should change status from SCHEDULED to CANCELLED")
    void shouldCancelScheduledAppointment() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        Appointment created = appointmentService.create(
                new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 30, null));

        Appointment cancelled = appointmentService.cancel(created.getId());
        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());

        Appointment reloaded = appointmentService.get(created.getId());
        assertEquals(AppointmentStatus.CANCELLED, reloaded.getStatus());
    }

    @Test
    @DisplayName("cancel() should throw ConflictException when trying to cancel COMPLETED or already CANCELLED appointment")
    void shouldThrowConflictWhenCancellingCompletedOrCancelled() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        Appointment created = appointmentService.create(
                new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 30, null));

        appointmentService.cancel(created.getId());

        // Repeat cancel
        ConflictException exCancel = assertThrows(ConflictException.class, () -> appointmentService.cancel(created.getId()));
        assertEquals("Appointment is already cancelled", exCancel.getMessage());

        // Cancel completed
        Appointment a2 = appointmentService.create(
                new Appointment(null, unvaccinatedCat.getId(), "Dr. Watson", baseTime, 30, null));
        appointmentService.complete(a2.getId());

        ConflictException exComplete = assertThrows(ConflictException.class, () -> appointmentService.cancel(a2.getId()));
        assertEquals("Cannot cancel a completed appointment", exComplete.getMessage());
    }

    @Test
    @DisplayName("cancel() should free up doctor's time slot for rebooking")
    void shouldAllowRebookingTimeSlotOfCancelledAppointment() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0);
        Appointment initial = appointmentService.create(
                new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 60, null));

        // Cancel first appointment
        appointmentService.cancel(initial.getId());

        // New booking at exact same time with same doctor must succeed
        Appointment rebooked = appointmentService.create(
                new Appointment(null, unvaccinatedCat.getId(), "Dr. House", baseTime, 60, null));

        assertNotNull(rebooked.getId());
        assertEquals(AppointmentStatus.SCHEDULED, rebooked.getStatus());
    }

    @Test
    @DisplayName("update() should update appointment details without conflicting with itself")
    void shouldUpdateAppointmentSuccessfullyWithoutSelfConflict() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(15).withMinute(0);
        Appointment created = appointmentService.create(
                new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 30, null));

        // Update keeping exact same time slot and doctor, changing duration
        Appointment updateData = new Appointment(null, unvaccinatedCat.getId(), "Dr. House", baseTime, 45, null);
        Appointment updated = appointmentService.update(created.getId(), updateData);

        assertEquals(created.getId(), updated.getId());
        assertEquals(unvaccinatedCat.getId(), updated.getPetId());
        assertEquals(45, updated.getDurationMinutes());
    }

    @Test
    @DisplayName("update() should throw ConflictException if updated time overlaps with another appointment")
    void shouldThrowConflictWhenUpdatedAppointmentOverlapsAnother() {
        LocalDateTime t1 = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        LocalDateTime t2 = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0);

        Appointment a1 = appointmentService.create(new Appointment(null, vaccinatedDog.getId(), "Dr. House", t1, 60, null));
        Appointment a2 = appointmentService.create(new Appointment(null, unvaccinatedCat.getId(), "Dr. House", t2, 60, null));

        // Try to update a2 to overlap with a1 (10:30 - 11:30)
        Appointment updatedA2 = new Appointment(null, unvaccinatedCat.getId(), "Dr. House", t1.plusMinutes(30), 60, null);

        assertThrows(ConflictException.class, () -> appointmentService.update(a2.getId(), updatedA2));
    }

    @Test
    @DisplayName("update() should throw ConflictException when trying to update non-SCHEDULED appointment")
    void shouldThrowConflictWhenUpdatingNonScheduledAppointment() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        Appointment created = appointmentService.create(
                new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 30, null));

        appointmentService.complete(created.getId());

        Appointment updateData = new Appointment(null, vaccinatedDog.getId(), "Dr. Watson", baseTime, 30, null);
        assertThrows(ConflictException.class, () -> appointmentService.update(created.getId(), updateData));
    }

    @Test
    @DisplayName("update() should throw ConflictException when updating pet to unvaccinated dog")
    void shouldThrowConflictWhenUpdatingToUnvaccinatedDog() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        Appointment created = appointmentService.create(
                new Appointment(null, unvaccinatedCat.getId(), "Dr. House", baseTime, 30, null));

        Appointment updateToUnvaccinatedDog = new Appointment(null, unvaccinatedDog.getId(), "Dr. House", baseTime, 30, null);
        assertThrows(ConflictException.class, () -> appointmentService.update(created.getId(), updateToUnvaccinatedDog));
    }

    @Test
    @DisplayName("delete() should remove appointment by id or throw NotFoundException")
    void shouldDeleteAppointmentOrThrowNotFound() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        Appointment created = appointmentService.create(
                new Appointment(null, vaccinatedDog.getId(), "Dr. House", baseTime, 30, null));

        appointmentService.delete(created.getId());
        assertThrows(NotFoundException.class, () -> appointmentService.get(created.getId()));

        assertThrows(NotFoundException.class, () -> appointmentService.delete("missing-id"));
    }
}
