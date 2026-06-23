package om.library.libraryapiapp.controllers;


import om.library.libraryapiapp.models.Employee;
import om.library.libraryapiapp.service.BorrowService;
import om.library.libraryapiapp.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final BorrowService borrowService;

    // POST /api/employees → 201 on success
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Employee create(@RequestBody Employee employee) {
        return employeeService.create(employee);
    }

    // GET /api/employees/{employeeId}/activity
    // → returns employee's active borrows and reservations
    @GetMapping("/{employeeId}/activity")
    public ResponseEntity<Map<String, Object>> getActivity(@PathVariable Long employeeId) {
        return ResponseEntity.ok(borrowService.getEmployeeActivity(employeeId));
    }
}