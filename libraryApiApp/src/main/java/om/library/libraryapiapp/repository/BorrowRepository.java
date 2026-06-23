package om.library.libraryapiapp.repository;

import om.library.libraryapiapp.models.Borrow;
import om.library.libraryapiapp.models.Employee;
import om.library.libraryapiapp.models.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Long> {
    Optional<Borrow> findByEmployeeAndResource(Employee employee, Resource resource);
    List<Borrow> findByEmployee(Employee employee);
}