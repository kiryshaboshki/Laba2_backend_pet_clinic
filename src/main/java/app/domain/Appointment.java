package app.domain;

import java.time.LocalDateTime;

public class Appointment {
    private String id;
    private String petId;
    private String vetName;
    private LocalDateTime dateTime;
    private int durationMinutes;
    private AppointmentStatus status;

    public Appointment() {}

    public Appointment(String id, String petId, String vetName, LocalDateTime dateTime, int durationMinutes, AppointmentStatus status) {
        this.id = id;
        this.petId = petId;
        this.vetName = vetName;
        this.dateTime = dateTime;
        this.durationMinutes = durationMinutes;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPetId() {
        return petId;
    }

    public void setPetId(String petId) {
        this.petId = petId;
    }

    public String getVetName() {
        return vetName;
    }

    public void setVetName(String vetName) {
        this.vetName = vetName;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}
