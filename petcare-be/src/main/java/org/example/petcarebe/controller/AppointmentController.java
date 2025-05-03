package org.example.petcarebe.controller;

import org.example.petcarebe.dto.AppointmentRequest;
import org.example.petcarebe.dto.AppointmentResponse;
import org.example.petcarebe.dto.CancelAppointmentsRequest;
import org.example.petcarebe.dto.PetResponse;
import org.example.petcarebe.dto.AppointmentHistoryDTO;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.service.AppointmentService;
import org.example.petcarebe.service.AppointmentHistoryService;
import org.example.petcarebe.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentHistoryService appointmentHistoryService;

    @PostMapping
    public ResponseEntity<?> createAppointment(@RequestBody AppointmentRequest request) {
        try {
            System.out.println("Creating appointment with request: " + request.toString());

            if (request.getTime() != null && !request.getTime().contains(":")) {
                request.setTime(request.getTime() + ":00");
            }

            Appointment appointment = appointmentService.createAppointment(request);
            return ResponseEntity.ok(new AppointmentResponse(appointment.getAppointmentId(), "PAID", "Lịch hẹn đã được lưu"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new AppointmentResponse(null, "FAILED", e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AppointmentResponse>> getPendingAppointments() {
        try {
            List<AppointmentResponse> pendingAppointments = appointmentService.getPendingAppointments();
            return ResponseEntity.ok(pendingAppointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{appointmentId}/pets")
    public ResponseEntity<List<PetResponse>> getPetsByAppointmentId(@PathVariable Long appointmentId) {
        try {
            List<PetResponse> pets = appointmentService.getPetsByAppointmentId(appointmentId);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmAppointments(@RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> appointmentIdsRaw = (List<Object>) payload.get("appointmentIds");
            if (appointmentIdsRaw == null || appointmentIdsRaw.isEmpty()) {
                throw new IllegalArgumentException("appointmentIds không được rỗng");
            }
            List<Long> appointmentIds = appointmentIdsRaw.stream()
                    .map(id -> {
                        if (id instanceof Number) {
                            return ((Number) id).longValue();
                        } else if (id instanceof String) {
                            return Long.parseLong((String) id);
                        } else {
                            throw new IllegalArgumentException("appointmentId phải là số: " + id);
                        }
                    })
                    .collect(Collectors.toList());

            Object userIdObj = payload.get("userId");
            Long userId = null;
            if (userIdObj != null) {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        userId = userService.findByEmail((String) userIdObj).getUserId();
                    }
                } else {
                    throw new IllegalArgumentException("userId phải là số hoặc email");
                }
            }
            if (userId == null) {
                throw new IllegalArgumentException("userId hoặc email không hợp lệ");
            }
            appointmentService.confirmAppointments(appointmentIds, userId);
            return ResponseEntity.ok(new AppointmentResponse(null, "CONFIRMED", "Đã xác nhận các lịch hẹn"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(null, "FAILED", "Lỗi xác nhận lịch hẹn: " + e.getMessage()));
        }
    }

    @PostMapping("/{appointmentId}/start")
    public ResponseEntity<?> startService(
            @PathVariable Long appointmentId,
            @RequestBody Map<Long, Long> petAssignments,
            @RequestParam(value = "userId") Object userIdObj) {
        try {
            Long userId = null;
            if (userIdObj != null) {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        userId = userService.findByEmail((String) userIdObj).getUserId();
                    }
                } else {
                    throw new IllegalArgumentException("userId phải là số hoặc email");
                }
            }
            if (userId == null) {
                throw new IllegalArgumentException("userId hoặc email không hợp lệ");
            }

            appointmentService.startService(appointmentId, userId, petAssignments);
            return ResponseEntity.ok(new AppointmentResponse(appointmentId, "IN_PROGRESS", "Bắt đầu dịch vụ thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(appointmentId, "FAILED", e.getMessage()));
        }
    }

    @PostMapping("/{appointmentId}/complete")
    public ResponseEntity<?> completeService(
            @PathVariable Long appointmentId,
            @RequestParam(value = "userId") Object userIdObj) {
        try {
            Long userId = null;
            if (userIdObj != null) {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        userId = userService.findByEmail((String) userIdObj).getUserId();
                    }
                } else {
                    throw new IllegalArgumentException("userId phải là số hoặc email");
                }
            }
            if (userId == null) {
                throw new IllegalArgumentException("userId hoặc email không hợp lệ");
            }

            appointmentService.completeService(appointmentId, userId);
            return ResponseEntity.ok(new AppointmentResponse(appointmentId, "COMPLETED", "Hoàn thành dịch vụ thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(appointmentId, "FAILED", e.getMessage()));
        }
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable Long appointmentId) {
        try {
            AppointmentResponse appointment = appointmentService.getAppointmentById(appointmentId);
            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/confirmed-by-date-and-time")
    public ResponseEntity<List<AppointmentResponse>> getConfirmedAppointmentsByDateAndTime(
            @RequestParam("date") String date,
            @RequestParam("time") String time) {
        try {
            LocalDate localDate = LocalDate.parse(date);

            LocalTime localTime;
            try {
                localTime = LocalTime.parse(time);
            } catch (Exception e) {
                if (!time.contains(":")) {
                    try {
                        localTime = LocalTime.parse(time + ":00");
                    } catch (Exception e2) {
                        if (time.length() == 1) {
                            localTime = LocalTime.parse("0" + time + ":00");
                        } else {
                            throw new IllegalArgumentException("Invalid time format: " + time);
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Invalid time format: " + time);
                }
            }

            List<AppointmentResponse> appointments = appointmentService.getConfirmedAppointmentsByDateAndTime(localDate, localTime);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/confirmed")
    public ResponseEntity<List<AppointmentResponse>> getConfirmedAppointmentsByDate(
            @RequestParam("date") String date) {
        try {
            List<AppointmentResponse> appointments = appointmentService.getConfirmedAppointmentsByDate(date);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<AppointmentResponse>> getActiveAppointmentsByDate(
            @RequestParam("date") String date) {
        try {
            List<AppointmentResponse> appointments = appointmentService.getActiveAppointmentsByDate(date);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/active-by-date-and-time")
    public ResponseEntity<List<AppointmentResponse>> getActiveAppointmentsByDateAndTime(
            @RequestParam("date") String date,
            @RequestParam("time") String time) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            LocalTime localTime;
            try {
                localTime = LocalTime.parse(time);
            } catch (Exception e) {
                if (!time.contains(":")) {
                    try {
                        localTime = LocalTime.parse(time + ":00");
                    } catch (Exception e2) {
                        if (time.length() == 1) {
                            localTime = LocalTime.parse("0" + time + ":00");
                        } else {
                            throw new IllegalArgumentException("Invalid time format: " + time);
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Invalid time format: " + time);
                }
            }
            List<AppointmentResponse> appointments = appointmentService.getActiveAppointmentsByDateAndTime(localDate, localTime);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/cancel")
    public ResponseEntity<?> cancelAppointments(@RequestBody CancelAppointmentsRequest request) {
        try {
            Object userIdObj = request.getUserId();
            Long userId = null;
            if (userIdObj != null) {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        userId = userService.findByEmail((String) userIdObj).getUserId();
                    }
                } else {
                    throw new IllegalArgumentException("userId phải là số hoặc email");
                }
            }
            if (userId == null) {
                throw new IllegalArgumentException("userId hoặc email không hợp lệ");
            }

            List<AppointmentResponse> responses = appointmentService.cancelAppointments(
                    request.getAppointmentIds(), request.getReason(), userId);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(null, "FAILED", e.getMessage()));
        }
    }

    @PutMapping("/cancel/paid")
    public ResponseEntity<?> cancelPaidAppointments(@RequestBody CancelAppointmentsRequest request) {
        try {
            Object userIdObj = request.getUserId();
            Long userId = null;
            if (userIdObj != null) {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        userId = userService.findByEmail((String) userIdObj).getUserId();
                    }
                } else {
                    throw new IllegalArgumentException("userId phải là số hoặc email");
                }
            }
            if (userId == null) {
                throw new IllegalArgumentException("userId hoặc email không hợp lệ");
            }

            List<AppointmentResponse> responses = appointmentService.cancelPaidAppointments(
                    request.getAppointmentIds(), request.getReason(), userId);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(null, "FAILED", e.getMessage()));
        }
    }

    @PutMapping("/cancel/confirmed")
    public ResponseEntity<?> cancelConfirmedAppointments(@RequestBody CancelAppointmentsRequest request) {
        try {
            Object userIdObj = request.getUserId();
            Long userId = null;
            if (userIdObj != null) {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        userId = userService.findByEmail((String) userIdObj).getUserId();
                    }
                } else {
                    throw new IllegalArgumentException("userId phải là số hoặc email");
                }
            }
            if (userId == null) {
                throw new IllegalArgumentException("userId hoặc email không hợp lệ");
            }

            List<AppointmentResponse> responses = appointmentService.cancelConfirmedAppointments(
                    request.getAppointmentIds(), request.getReason(), userId);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(null, "FAILED", e.getMessage()));
        }
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<?> updateAppointment(@PathVariable Long appointmentId, @RequestBody Map<String, Object> request) {
        try {
            System.out.println("Updating appointment: " + appointmentId);
            System.out.println("Request: " + request.toString());

            String date = (String) request.get("date");
            String time = (String) request.get("time");
            String currentDate = (String) request.get("currentDate");
            String currentTime = (String) request.get("currentTime");
            String note = (String) request.get("note");
            Object userIdObj = request.get("userId");
            Long userId = null;
            if (userIdObj != null) {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        userId = userService.findByEmail((String) userIdObj).getUserId();
                    }
                } else {
                    throw new IllegalArgumentException("userId phải là số hoặc email");
                }
            }
            if (userId == null) {
                throw new IllegalArgumentException("userId hoặc email không hợp lệ");
            }

            AppointmentRequest appointmentRequest = new AppointmentRequest();
            appointmentRequest.setDate(date);
            appointmentRequest.setTime(time);
            appointmentRequest.setCurrentDate(currentDate);
            appointmentRequest.setCurrentTime(currentTime);
            appointmentRequest.setNote(note);

            if (appointmentRequest.getTime() != null && !appointmentRequest.getTime().contains(":")) {
                appointmentRequest.setTime(appointmentRequest.getTime() + ":00");
            }

            Appointment updatedAppointment = appointmentService.updateAppointment(appointmentId, appointmentRequest, userId);
            return ResponseEntity.ok(new AppointmentResponse(updatedAppointment.getAppointmentId(), "UPDATED", "Cập nhật lịch hẹn thành công"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new AppointmentResponse(null, "FAILED", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<AppointmentHistoryDTO>> getAppointmentHistory() {
        try {
            List<AppointmentHistoryDTO> history = appointmentHistoryService.findAllHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/history/search")
    public ResponseEntity<List<AppointmentHistoryDTO>> searchHistoryByPhone(@RequestParam("phone") String phone) {
        try {
            List<AppointmentHistoryDTO> history = appointmentHistoryService.findHistoryByPhone(phone);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/refunded")
    public ResponseEntity<List<AppointmentResponse>> getRefundedAppointments() {
        try {
            List<AppointmentResponse> refundedAppointments = appointmentService.getRefundedAppointments();
            return ResponseEntity.ok(refundedAppointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/refunded/pending-count")
    public ResponseEntity<Long> getRefundedAppointmentsPendingCount() {
        try {
            long count = appointmentService.getRefundedAppointmentsPendingCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{appointmentId}/refund")
    public ResponseEntity<?> updateRefundStatus(@PathVariable Long appointmentId, @RequestBody Map<String, Object> request) {
        try {
            String refundStatus = (String) request.get("refundStatus");
            String refundMethod = (String) request.get("refundMethod");
            String refundNote = (String) request.get("refundNote");
            Object userIdObj = request.get("userId");
            Long userId = null;
            if (userIdObj != null) {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        userId = userService.findByEmail((String) userIdObj).getUserId();
                    }
                } else {
                    throw new IllegalArgumentException("userId phải là số hoặc email");
                }
            }
            if (userId == null) {
                throw new IllegalArgumentException("userId hoặc email không hợp lệ");
            }

            appointmentService.updateRefundStatus(appointmentId, refundStatus, refundMethod, refundNote, userId);
            return ResponseEntity.ok(new AppointmentResponse(appointmentId, "UPDATED", "Cập nhật trạng thái hoàn tiền thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(appointmentId, "FAILED", e.getMessage()));
        }
    }

    @DeleteMapping("/{appointmentId}/pets/{petId}")
    public ResponseEntity<?> removePetFromAppointment(
            @PathVariable Long appointmentId,
            @PathVariable Long petId,
            @RequestBody Map<String, Object> request) {
        try {
            Object userIdObj = request.get("userId");
            Long userId = null;
            if (userIdObj != null) {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        userId = userService.findByEmail((String) userIdObj).getUserId();
                    }
                } else {
                    throw new IllegalArgumentException("userId phải là số hoặc email");
                }
            }
            if (userId == null) {
                throw new IllegalArgumentException("userId hoặc email không hợp lệ");
            }

            AppointmentResponse response = appointmentService.removePetFromAppointment(appointmentId, petId, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(appointmentId, "FAILED", e.getMessage()));
        }
    }
}