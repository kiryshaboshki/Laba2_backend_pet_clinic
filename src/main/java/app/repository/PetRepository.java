package app.repository;

import app.domain.Pet;

import java.util.List;
import java.util.Optional;

public interface PetRepository {

    List<Pet> findAll();

    Optional<Pet> findById(String id);

    Pet save(Pet pet);

    void deleteById(String id);
}
