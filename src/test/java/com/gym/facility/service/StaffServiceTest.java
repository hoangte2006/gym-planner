
package com.gym.facility.service;

import com.gym.core.dao.StaffDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Manager;
import com.gym.core.model.Staff;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StaffServiceTest {

    private StaffDao staffDao;
    private StaffService staffService;
    private Manager manager;

    @BeforeEach
    void setUp() {
        staffDao = new StaffDao();
        staffService = new StaffService(staffDao);

        manager = new Manager();
        manager.setFullName("TEST MANAGER");
        manager.setPhone(generateTestPhone());
        manager.setSalary(new BigDecimal("15000000"));
    }

    @AfterEach
    void tearDown() {
        Integer staffId = manager.getStaffId();

        if (staffId != null && staffId > 0) {
            staffDao.deleteById(staffId);
        }
    }

    private String generateTestPhone() {
        String digits = UUID.randomUUID()
                .toString()
                .replace("-", "");

        long number = Long.parseLong(digits.substring(0, 12), 16)
                % 100_000_000L;

        return String.format("09%08d", number);
    }

    @Test
    void createStaff_shouldCreateManagerSuccessfully() {
        Staff result = staffService.createStaff(manager);

        assertNotNull(result);
        assertTrue(result.getStaffId() > 0);
        assertEquals("TEST MANAGER", result.getFullName());
        assertEquals(manager.getPhone(), result.getPhone());

        Optional<Staff> saved =
                staffDao.findById(result.getStaffId());

        assertTrue(saved.isPresent());
        assertEquals(result.getPhone(), saved.get().getPhone());
    }

    @Test
    void createStaff_shouldRejectNullStaff() {
        assertThrows(
                BusinessException.class,
                () -> staffService.createStaff(null)
        );
    }

    @Test
    void createStaff_shouldRejectBlankName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.setFullName(" ")
        );
    }

    @Test
    void setPhone_shouldRejectBlankPhone() {
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.setPhone(" ")
        );
    }

    @Test
    void findByPhone_shouldReturnCreatedStaff() {
        staffService.createStaff(manager);

        Optional<Staff> result =
                staffDao.findByPhone(manager.getPhone());

        assertTrue(result.isPresent());
        assertEquals(manager.getFullName(), result.get().getFullName());
    }
}
