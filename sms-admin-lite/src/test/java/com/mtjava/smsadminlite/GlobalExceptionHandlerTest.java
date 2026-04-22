package com.mtjava.smsadminlite;

import com.mtjava.smsadminlite.common.BusinessException;
import com.mtjava.smsadminlite.common.GlobalExceptionHandler;
import com.mtjava.smsadminlite.controller.UserController;
import com.mtjava.smsadminlite.model.User;
import com.mtjava.smsadminlite.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn404WhenBusinessResourceIsMissing() throws Exception {
        when(userService.getUserById(99L)).thenThrow(BusinessException.notFound("用户不存在，id=99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("用户不存在，id=99"));
    }

    @Test
    void shouldKeepSuccessBodyWhenRequestSucceeds() throws Exception {
        when(userService.listUsers()).thenReturn(List.of(new User(1L, "张三", "13800000000", LocalDateTime.now())));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].name").value("张三"));
    }
}
