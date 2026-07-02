package com.bizdevar.seller;

import java.util.LinkedHashMap;
import java.util.Map;

public class SellerAccount {
    public long id;
    public String email;
    public String phone;
    public String passwordHash;
    public String ownerName;
    public String ownerSurname;
    public String storeName;
    public String category;
    public String storeType;
    public String voen;
    public String verificationStatus;
    public String status;
    public String rejectionReason;
    public String rejectedAt;
    public String approvedAt;
    public boolean autoNamed;
    public double revenue;
    public double rating;
    public long vendorId;
    public String logoUrl;
    public String storeCode;
    public String storeSlug;
    public String ownerSlug;
    public String createdAt;
    public String ownerLoginPasswordHash;
    public boolean ownerFaceEnrolled;
    public String ownerFaceSubject;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("email", email);
        if (phone != null && !phone.isBlank()) m.put("phone", phone);
        m.put("store_name", storeName);
        m.put("owner_name", ownerName);
        m.put("owner_surname", ownerSurname);
        m.put("category", category);
        m.put("store_type", storeType);
        if (voen != null && !voen.isBlank()) m.put("voen", voen);
        m.put("verification_status", verificationStatus);
        m.put("status", status);
        m.put("auto_named", autoNamed);
        m.put("revenue", revenue);
        m.put("rating", rating);
        if (vendorId > 0) m.put("vendor_id", vendorId);
        if (logoUrl != null && !logoUrl.isBlank()) m.put("logo_url", logoUrl);
        if (storeSlug != null && !storeSlug.isBlank()) m.put("store_slug", storeSlug);
        if (ownerSlug != null && !ownerSlug.isBlank()) m.put("owner_slug", ownerSlug);
        if (createdAt != null) m.put("created_at", createdAt);
        if (rejectionReason != null && !rejectionReason.isBlank()) m.put("rejection_reason", rejectionReason);
        if (rejectedAt != null) m.put("rejected_at", rejectedAt);
        if (approvedAt != null) m.put("approved_at", approvedAt);
        return m;
    }
}
