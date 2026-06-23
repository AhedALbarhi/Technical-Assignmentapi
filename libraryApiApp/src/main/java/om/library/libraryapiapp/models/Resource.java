package om.library.libraryapiapp.models;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class Resource {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String title;

        @Enumerated(EnumType.STRING)
        private ResourceType type; // BOOK, EQUIPMENT, AV_KIT

        private int totalCopies;
        private int availableCopies;

        public enum ResourceType {
            BOOK, EQUIPMENT, AV_KIT
        }
    }

