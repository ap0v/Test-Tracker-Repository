package com.example.testtracker.Folder;

import java.util.UUID;
import jakarta.validation.constraints.Pattern;

// Fields left out of the request keep their current value. For parentId, null already means
// "move to the top level", so the setter records whether the field was sent at all.
public class UpdateFolderRequest {
    @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "Name cannot be blank")
    private String name;

    private UUID parentId;

    private boolean parentIdSent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
        this.parentIdSent = true;
    }

    public boolean hasParentId() {
        return parentIdSent;
    }
}
