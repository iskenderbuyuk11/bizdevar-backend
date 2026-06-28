package com.bizdevar.seller;

import java.util.LinkedHashMap;
import java.util.Map;

public class VendorInfo {
    public long id;
    public String name;
    public String category;
    public String status;
    public String verificationStatus;
    public String storeType;
    public String rejectionReason;
    public boolean autoNamed;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("category", category);
        m.put("status", status);
        m.put("verification_status", verificationStatus);
        m.put("store_type", storeType);
        if (rejectionReason != null && !rejectionReason.isBlank()) m.put("rejection_reason", rejectionReason);
        m.put("auto_named", autoNamed);
        return m;
    }
}
