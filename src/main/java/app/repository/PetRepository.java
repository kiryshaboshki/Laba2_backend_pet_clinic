package app.repository;

import app.domain.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PetRepository extends JpaRepository<Pet, UUID> {
}
