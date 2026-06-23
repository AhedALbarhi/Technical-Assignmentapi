package om.library.libraryapiapp.service;

import om.library.libraryapiapp.models.*;
import om.library.libraryapiapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private final ResourceRepository resourceRepo;
    private final EmployeeRepository employeeRepo;
    private final BorrowRepository borrowRepo;
    private final WaitlistEntryRepository waitlistRepo;
    private final ReservationRepository reservationRepo;

    @Transactional
    public String borrow(Long resourceId, Long employeeId) {
        Resource resource = resourceRepo.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (borrowRepo.findByEmployeeAndResource(employee, resource).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already hold this resource");
        }

        if (resource.getAvailableCopies() > 0) {
            resource.setAvailableCopies(resource.getAvailableCopies() - 1);
            resourceRepo.save(resource);
            Borrow borrow = new Borrow(null, employee, resource, LocalDateTime.now());
            borrowRepo.save(borrow);
            return "Borrowed successfully";
        } else {
            if (waitlistRepo.existsByEmployeeAndResource(employee, resource)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already on the waitlist");
            }
            WaitlistEntry entry = new WaitlistEntry(null, employee, resource, LocalDateTime.now());
            waitlistRepo.save(entry);
            return "No copies available. Added to waitlist";
        }
    }

    @Transactional
    public String returnResource(Long resourceId, Long employeeId) {
        Resource resource = resourceRepo.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        Borrow borrow = borrowRepo.findByEmployeeAndResource(employee, resource)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "You don't hold this resource"));

        borrowRepo.delete(borrow);

        List<WaitlistEntry> waitlist = waitlistRepo.findByResourceOrderByJoinedAtAsc(resource);
        if (!waitlist.isEmpty()) {
            WaitlistEntry first = waitlist.get(0);
            waitlistRepo.delete(first);
            Reservation reservation = new Reservation(
                    null,
                    first.getEmployee(),
                    resource,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(24),
                    false
            );
            reservationRepo.save(reservation);
        } else {
            resource.setAvailableCopies(resource.getAvailableCopies() + 1);
            resourceRepo.save(resource);
        }
        return "Returned successfully";
    }

    @Transactional
    public String claimReservation(Long reservationId, Long employeeId) {
        Reservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

        if (!reservation.getEmployee().getId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This reservation is not yours");
        }
        if (reservation.isClaimed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already claimed");
        }
        if (LocalDateTime.now().isAfter(reservation.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation has expired");
        }

        reservation.setClaimed(true);
        reservationRepo.save(reservation);

        Borrow borrow = new Borrow(null, reservation.getEmployee(), reservation.getResource(), LocalDateTime.now());
        borrowRepo.save(borrow);
        return "Reservation claimed, resource is now borrowed";
    }

    @Transactional
    public String processExpired() {
        List<Reservation> allReservations = reservationRepo.findAll();
        int processed = 0;

        for (Reservation r : allReservations) {
            if (!r.isClaimed() && LocalDateTime.now().isAfter(r.getExpiresAt())) {
                Resource resource = r.getResource();
                reservationRepo.delete(r);

                List<WaitlistEntry> waitlist = waitlistRepo.findByResourceOrderByJoinedAtAsc(resource);
                if (!waitlist.isEmpty()) {
                    WaitlistEntry next = waitlist.get(0);
                    waitlistRepo.delete(next);
                    Reservation newReservation = new Reservation(
                            null,
                            next.getEmployee(),
                            resource,
                            LocalDateTime.now(),
                            LocalDateTime.now().plusHours(24),
                            false
                    );
                    reservationRepo.save(newReservation);
                } else {
                    resource.setAvailableCopies(resource.getAvailableCopies() + 1);
                    resourceRepo.save(resource);
                }
                processed++;
            }
        }
        return "Processed " + processed + " expired reservation(s)";
    }

    public List<WaitlistEntry> getWaitlist(Long resourceId) {
        Resource resource = resourceRepo.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        return waitlistRepo.findByResourceOrderByJoinedAtAsc(resource);
    }

    @Transactional
    public String leaveWaitlist(Long resourceId, Long employeeId) {
        Resource resource = resourceRepo.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        WaitlistEntry entry = waitlistRepo.findByEmployeeAndResource(employee, resource)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not on this waitlist"));

        waitlistRepo.delete(entry);
        return "Removed from waitlist";
    }

    public Map<String, Object> getEmployeeActivity(Long employeeId) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("borrows", borrowRepo.findByEmployee(employee));
        result.put("reservations", reservationRepo.findByEmployee(employee));
        return result;
    }
}