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
}
