package om.library.libraryapiapp.service;

import om.library.libraryapiapp.models.Employee;
import om.library.libraryapiapp.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepo;

    public Employee create(Employee employee) {
        return employeeRepo.save(employee);
    }

    public Employee getById(Long id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    public List<Employee> getAll() {
        return employeeRepo.findAll();
    }
}