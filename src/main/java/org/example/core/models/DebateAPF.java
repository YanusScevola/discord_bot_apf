package org.example.core.models;

import java.time.LocalDateTime;
import java.util.List;

public class DebateAPF {
    private String id;
    private List<Long> governmentDebatersIds;
    private List<Long> oppositionDebatersIds;
    private LocalDateTime dateTime;
    private String winner; // "government" or "opposition"


}