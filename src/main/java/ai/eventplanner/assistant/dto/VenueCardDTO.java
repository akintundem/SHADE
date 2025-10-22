package ai.eventplanner.assistant.dto;

import lombok.Data;
import java.util.List;

@Data
public class VenueCardDTO {
    private String id;
    private String name;
    private String location;
    private String imageUrl;
    private Double rating;
    private Integer reviewCount;
    private String guestCapacity;
    private String priceRange;
    private String description;
    private List<String> amenities;
    private String contactEmail;
    private String contactPhone;
    private String website;
}


