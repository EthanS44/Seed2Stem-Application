package com.example.seed2stem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskRepository repo;

    @InjectMocks
    private TaskController controller;

    @Test
    void getTasks_returnsAllTasks() {
        Task task1 = new Task("Task 1", "Description 1", null);
        Task task2 = new Task("Task 2", "Description 2", null);
        when(repo.findAll()).thenReturn(List.of(task1, task2));

        List<Task> result = controller.getTasks();

        assertEquals(2, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
        assertEquals("Task 2", result.get(1).getTitle());
    }

    @Test
    void getTasks_emptyList_returnsEmpty() {
        when(repo.findAll()).thenReturn(List.of());

        List<Task> result = controller.getTasks();

        assertTrue(result.isEmpty());
    }

    @Test
    void createTask_savesAndReturnsTask() {
        Task task = new Task("New Task", "New Description", null);
        when(repo.save(any(Task.class))).thenAnswer(inv -> {
            Task saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Task result = controller.createTask(task);

        assertNotNull(result);
        assertEquals("New Task", result.getTitle());
        assertEquals("New Description", result.getDescription());
        verify(repo).save(task);
    }
}
