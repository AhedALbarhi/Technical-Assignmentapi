package om.library.libraryapiapp.controllers;

import om.library.libraryapiapp.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final BorrowService borrowService;

    // POST /api/reservations/{reservationId}/claim
    // → must be claimed within 24hrs, only by the right employee
    @PostMapping("/{reservationId}/claim")
    public ResponseEntity<String> claim(@PathVariable Long reservationId,
                                        @RequestParam Long employeeId) {
        return ResponseEntity.ok(borrowService.claimReservation(reservationId, employeeId));
    }

    // POST /api/reservations/process-expired
    // → manually triggers expiry check and cascades queue forward
    @PostMapping("/process-expired")
    public ResponseEntity<String> processExpired() {
        return ResponseEntity.ok(borrowService.processExpired());
    }
}