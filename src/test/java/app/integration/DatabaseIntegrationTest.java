package app.integration;

import app.domain.Appointment;
import app.domain.AppointmentStatus;
import app.domain.Pet;
import app.domain.PetType;
import app.exception.ConflictException;
import app.repository.AppointmentRepository;
import app.repository.PetRepository;
import app.service.AppointmentService;
import app.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Database Integration Tests - Persistence, Transactions & Relationships")
class DatabaseIntegrationTest {

    @Autowired
    private PetService petService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @BeforeEach
    void cleanUp() {
        appointmentRepository.deleteAll();
        petRepository.deleteAll();
    }

    @Test
    @Transactional
    @DisplayName("Requirement 1 & 2: Persist related Pet and Appointment with foreign key integrity")
    void shouldPersistPetAndAppointmentRelationship() {
        Pet pet = new Pet(null, "Murka", PetType.CAT, "Elena", true);
        Pet savedPet = petService.create(pet);
        assertNotNull(savedPet.getId());

        LocalDateTime time = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        Appointment appointment = new Appointment(null, savedPet, "Dr. Aibolit", time, 30, AppointmentStatus.SCHEDULED, "Routine checkup");
        Appointment savedAppointment = appointmentService.create(appointment);
        assertNotNull(savedAppointment.getId());

        Appointment reloaded = appointmentRepository.findById(savedAppointment.getId()).orElseThrow();
        assertEquals("Dr. Aibolit", reloaded.getVetName());
        assertEquals("Routine checkup", reloaded.getNotes());
        assertEquals(savedPet.getId(), reloaded.getPet().getId());
        assertEquals("Murka", reloaded.getPet().getName());
    }

    @Test
    @DisplayName("Requirement 3: Atomic operation modifying multiple related resources with rollback on failure")
    void shouldRollbackAllChangesWhenErrorOccursInTransaction() {
        Pet pet = petService.create(new Pet(null, "Rex", PetType.DOG, "Original Owner", true));

        LocalDateTime time1 = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime time2 = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0);
        Appointment a1 = appointmentRepository.save(new Appointment(null, pet, "Dr. House", time1, 30, AppointmentStatus.SCHEDULED));
        Appointment a2 = appointmentRepository.save(new Appointment(null, pet, "Dr. Watson", time2, 30, AppointmentStatus.SCHEDULED));

        assertThrows(ConflictException.class, () ->
                petService.transferOwnership(pet.getId().toString(), "trigger-error"));

        Pet petAfterRollback = petRepository.findById(pet.getId()).orElseThrow();
        assertEquals("Original Owner", petAfterRollback.getOwnerName(),
                "Pet owner must NOT be modified when transaction rolls back");

        Appointment a1AfterRollback = appointmentRepository.findById(a1.getId()).orElseThrow();
        Appointment a2AfterRollback = appointmentRepository.findById(a2.getId()).orElseThrow();
        assertEquals(AppointmentStatus.SCHEDULED, a1AfterRollback.getStatus(),
                "Appointment status must remain SCHEDULED when transaction rolls back");
        assertEquals(AppointmentStatus.SCHEDULED, a2AfterRollback.getStatus(),
                "Appointment status must remain SCHEDULED when transaction rolls back");
        assertNull(a1AfterRollback.getNotes(), "Appointment notes must not be modified after rollback");
    }

    @Test
    @DisplayName("Requirement 5: Fetch list of resources with related objects without N+1 problem on 30+ items")
    void shouldFetchListWithRelatedEntitiesEfficiently() {
        List<Appointment> createdAppointments = new ArrayList<>();

        for (int i = 1; i <= 30; i++) {
            Pet pet = petRepository.save(new Pet(null, "Pet-" + i, PetType.CAT, "Owner-" + i, true));
            LocalDateTime time = LocalDateTime.now().plusDays(i).withHour(10).withMinute(0);
            Appointment appt = new Appointment(null, pet, "Dr. Vet-" + i, time, 30, AppointmentStatus.SCHEDULED, "Note " + i);
            createdAppointments.add(appointmentRepository.save(appt));
        }

        assertEquals(30, createdAppointments.size());

        List<Appointment> detailedList = appointmentService.listDetailed();

        assertEquals(30, detailedList.size());
        for (Appointment a : detailedList) {
            assertNotNull(a.getPet(), "Related pet must be loaded");
            assertNotNull(a.getPet().getName(), "Pet name must be populated via single JOIN FETCH");
        }
    }
}
