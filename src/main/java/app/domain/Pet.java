package app.domain;

public class Pet {
    private String id;
    private String name;
    private PetType type;
    private String ownerName;
    private boolean vaccinated;

    public Pet() {}

    public Pet(String id, String name, PetType type, String ownerName, boolean vaccinated) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.ownerName = ownerName;
        this.vaccinated = vaccinated;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public PetType getType() { return type; }
    public void setType(PetType type) { this.type = type; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public boolean isVaccinated() { return vaccinated; }
    public void setVaccinated(boolean vaccinated) { this.vaccinated = vaccinated; }
}