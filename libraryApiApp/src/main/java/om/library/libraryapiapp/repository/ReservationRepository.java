package om.library.libraryapiapp.repository;

import om.library.libraryapiapp.models.Employee;
import om.library.libraryapiapp.models.Reservation;
import om.library.libraryapiapp.models.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByResourceAndClaimedFalse(Resource resource);
    List<Reservation> findByEmployee(Employee employee);
}