package app.domain;

public enum PetType {
    CAT(false),  // кошкам прививка для записи не нужна
    DOG(true);   // собакам обязательна

    private final boolean vaccinationRequired;

    PetType(boolean vaccinationRequired) {
        this.vaccinationRequired = vaccinationRequired;
    }

    public boolean isVaccinationRequired() {
        return vaccinationRequired;
    }
}