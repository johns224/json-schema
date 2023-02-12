package org.rossjohnson.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAlert {
    private String customerId;
    private String title;
    private String message;
    private String destinationPhoneNumber;
    private String email;
    private String alertType;
}
