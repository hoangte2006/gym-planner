
package com.gym.facility.service;

import com.gym.core.dao.RoomDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Room;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoomServiceTest {

    private RoomDao roomDao;
    private RoomService roomService;
    private Room room;

    @BeforeEach
    void setUp() {
        roomDao = new RoomDao();
        roomService = new RoomService(roomDao);

        room = new Room();
        room.setRoomName("TEST-" + UUID.randomUUID());
        room.setCapacity(20);
        room.setFloorLocation("TEST FLOOR");
    }

    @AfterEach
    void tearDown() {
        if (room != null
                && room.getRoomId() != null
                && room.getRoomId() > 0) {

            roomDao.deleteById(room.getRoomId());
        }
    }

    @Test
    void createRoom_shouldCreateSuccessfully() {
        Room result = roomService.createRoom(room);

        assertNotNull(result);
        assertTrue(result.getRoomId() > 0);
        assertEquals(room.getRoomName(), result.getRoomName());
        assertEquals(20, result.getCapacity());

        Optional<Room> saved = roomDao.findById(result.getRoomId());

        assertTrue(saved.isPresent());
        assertEquals(result.getRoomName(), saved.get().getRoomName());
    }

    @Test
    void createRoom_shouldRejectNullRoom() {
        assertThrows(
                BusinessException.class,
                () -> roomService.createRoom(null)
        );
    }

    @Test
    void createRoom_shouldRejectBlankName() {


        assertThrows(
                IllegalArgumentException.class,
                () -> room.setRoomName("   ")
        );
    }

    @Test
    void createRoom_shouldRejectInvalidCapacity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> room.setCapacity(0)
        );
    }

    @Test
    void findRoomByName_shouldReturnRoom() {
        Room saved = roomService.createRoom(room);

        Optional<Room> result =
                roomService.findRoomByName(saved.getRoomName());

        assertTrue(result.isPresent());
        assertEquals(saved.getRoomId(), result.get().getRoomId());
    }

    @Test
    void updateRoom_shouldUpdateInformation() {
        Room saved = roomService.createRoom(room);

        saved.setCapacity(35);
        saved.setFloorLocation("TEST FLOOR 2");

        roomService.updateRoom(saved);

        Optional<Room> updated = roomDao.findById(saved.getRoomId());

        assertTrue(updated.isPresent());
        assertEquals(35, updated.get().getCapacity());
        assertEquals("TEST FLOOR 2", updated.get().getFloorLocation());
    }
}
