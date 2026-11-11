package app.controller;

import app.domain.Appointment;
import app.domain.AppointmentStatus;
import app.exception.BadRequestException;
import app.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<Appointment> list(
            @RequestParam(required = false) String vetName,
            @RequestParam(required = false) String status) {
        AppointmentStatus appointmentStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                appointmentStatus = AppointmentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status. Use SCHEDULED, COMPLETED or CANCELLED");
            }
        }
        return appointmentService.list(vetName, appointmentStatus);
    }

    @GetMapping("/{id}")
    public Appointment get(@PathVariable String id) {
        return appointmentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Appointment create(@RequestBody Appointment appointment) {
        return appointmentService.create(appointment);
    }

    @PutMapping("/{id}")
    public Appointment update(@PathVariable String id, @RequestBody Appointment appointment) {
        return appointmentService.update(id, appointment);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        appointmentService.delete(id);
    }

    @PostMapping("/{id}/complete")
    public Appointment complete(@PathVariable String id) {
        return appointmentService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public Appointment cancel(@PathVariable String id) {
        return appointmentService.cancel(id);
    }
}
