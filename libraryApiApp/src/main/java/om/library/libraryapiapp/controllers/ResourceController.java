package om.library.libraryapiapp.controllers;

import om.library.libraryapiapp.models.Resource;
import om.library.libraryapiapp.models.WaitlistEntry;
import om.library.libraryapiapp.service.BorrowService;
import om.library.libraryapiapp.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final BorrowService borrowService;

    // POST /api/resources → 201 on success
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Resource create(@RequestBody Resource resource) {
        return resourceService.create(resource);
    }

    // POST /api/resources/{resourceId}/borrow
    // → borrows if copy available, joins waitlist if not
    @PostMapping("/{resourceId}/borrow")
    public ResponseEntity<String> borrow(@PathVariable Long resourceId,
                                         @RequestParam Long employeeId) {
        return ResponseEntity.ok(borrowService.borrow(resourceId, employeeId));
    }

    // POST /api/resources/{resourceId}/return
    // → only the employee who holds it can return it
    @PostMapping("/{resourceId}/return")
    public ResponseEntity<String> returnResource(@PathVariable Long resourceId,
                                                 @RequestParam Long employeeId) {
        return ResponseEntity.ok(borrowService.returnResource(resourceId, employeeId));
    }

    // GET /api/resources/{resourceId}/waitlist
    // → returns waitlist in FIFO order
    @GetMapping("/{resourceId}/waitlist")
    public List<WaitlistEntry> getWaitlist(@PathVariable Long resourceId) {
        return borrowService.getWaitlist(resourceId);
    }

    // DELETE /api/resources/{resourceId}/waitlist/{employeeId}
    // → employee leaves waitlist voluntarily
    @DeleteMapping("/{resourceId}/waitlist/{employeeId}")
    public ResponseEntity<String> leaveWaitlist(@PathVariable Long resourceId,
                                                @PathVariable Long employeeId) {
        return ResponseEntity.ok(borrowService.leaveWaitlist(resourceId, employeeId));
    }
}