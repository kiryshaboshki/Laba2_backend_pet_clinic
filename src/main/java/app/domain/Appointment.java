package app.domain;

import java.time.LocalDateTime;

public class Appointment {
    private String id;
    private String petId;             // Связь с питомцем
    private String vetName;           // Имя врача
    private LocalDateTime dateTime;   // Дата и время приема
    private int durationMinutes;      // Длительность
    private AppointmentStatus status; //(SCHEDULED, COMPLETED, CANCELLED)

    public Appointment() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getVetName() { return vetName; }
    public void setVetName(String vetName) { this.vetName = vetName; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
}