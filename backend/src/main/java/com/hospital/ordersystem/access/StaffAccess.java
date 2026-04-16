package com.hospital.ordersystem.access;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource-Access layer: in-memory registry of fulfilment staff members.
 * Used by load-balancing triage to auto-assign orders.
 */
@Component
public class StaffAccess {

    private final Set<String> staffMembers = ConcurrentHashMap.newKeySet();

    public void addStaff(String staffId) {
        staffMembers.add(staffId);
    }

    public void removeStaff(String staffId) {
        staffMembers.remove(staffId);
    }

    public List<String> listAllStaff() {
        return new ArrayList<>(staffMembers);
    }

    public boolean isEmpty() {
        return staffMembers.isEmpty();
    }
}
