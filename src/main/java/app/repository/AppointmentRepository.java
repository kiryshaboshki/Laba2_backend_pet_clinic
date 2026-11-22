package app.repository;

import app.domain.Appointment;
import app.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPet_Id(UUID petId);

    boolean existsByPet_IdAndStatus(UUID petId, AppointmentStatus status);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"pet"})
    Optional<Appointment> findById(@NonNull UUID id);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"pet"})
    List<Appointment> findAll();

    @Query("SELECT a FROM Appointment a JOIN FETCH a.pet")
    List<Appointment> findAllWithPet();
}
