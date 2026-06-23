package om.library.libraryapiapp.repository;

import om.library.libraryapiapp.models.Employee;
import om.library.libraryapiapp.models.Resource;
import om.library.libraryapiapp.models.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {
    List<WaitlistEntry> findByResourceOrderByJoinedAtAsc(Resource resource);
    Optional<WaitlistEntry> findByEmployeeAndResource(Employee employee, Resource resource);
    boolean existsByEmployeeAndResource(Employee employee, Resource resource);
}