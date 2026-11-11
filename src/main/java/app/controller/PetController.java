package app.controller;

import app.domain.Pet;
import app.service.PetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public List<Pet> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean vaccinated) {
        return petService.list(type, vaccinated);
    }

    @GetMapping("/{id}")
    public Pet get(@PathVariable String id) {
        return petService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pet create(@RequestBody Pet pet) {
        return petService.create(pet);
    }

    @PutMapping("/{id}")
    public Pet update(@PathVariable String id, @RequestBody Pet pet) {
        return petService.update(id, pet);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        petService.delete(id);
    }
}
