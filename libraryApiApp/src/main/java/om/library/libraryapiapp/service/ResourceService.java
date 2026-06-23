package om.library.libraryapiapp.service;

import om.library.libraryapiapp.models.Resource;
import om.library.libraryapiapp.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepo;

    public Resource create(Resource resource) {
        resource.setAvailableCopies(resource.getTotalCopies());
        return resourceRepo.save(resource);
    }

    public Resource getById(Long id) {
        return resourceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
    }

    public List<Resource> getAll() {
        return resourceRepo.findAll();
    }
}