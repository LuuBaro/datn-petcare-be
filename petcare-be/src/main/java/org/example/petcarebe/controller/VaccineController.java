package org.example.petcarebe.controller;

import org.example.petcarebe.model.Vaccine;
import org.example.petcarebe.service.VaccineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vaccines")
@CrossOrigin(origins = "http://localhost:3000")
public class VaccineController {

    @Autowired
    private VaccineService vaccineService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Vaccine>> createVaccine(@RequestBody Vaccine vaccine) {
        try {
            Vaccine createdVaccine = vaccineService.createVaccine(vaccine);
            return new ResponseEntity<>(new ApiResponse<>(createdVaccine), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ApiResponse<>(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse<>("Lỗi server khi tạo vaccine"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getAllVaccines")
    public ResponseEntity<ApiResponse<List<Vaccine>>> getAllVaccines() {
        try {
            List<Vaccine> vaccines = vaccineService.getAllVaccines();
            return new ResponseEntity<>(new ApiResponse<>(vaccines), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse<>("Lỗi server khi lấy danh sách vaccine"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getVaccine/{id}")
    public ResponseEntity<ApiResponse<Vaccine>> getVaccineById(@PathVariable Long id) {
        try {
            Optional<Vaccine> vaccine = vaccineService.getVaccineById(id);
            if (vaccine.isPresent()) {
                return new ResponseEntity<>(new ApiResponse<>(vaccine.get()), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new ApiResponse<>("Vaccine không tồn tại"), HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse<>("Lỗi server khi lấy vaccine"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/updateVaccine/{id}")
    public ResponseEntity<ApiResponse<Vaccine>> updateVaccine(@PathVariable Long id, @RequestBody Vaccine vaccine) {
        try {
            Vaccine updatedVaccine = vaccineService.updateVaccine(id, vaccine);
            return new ResponseEntity<>(new ApiResponse<>(updatedVaccine), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ApiResponse<>(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(new ApiResponse<>(e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse<>("Lỗi server khi cập nhật vaccine"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/deleteVaccine/{id}")
    public ResponseEntity<ApiResponse<String>> toggleVaccineStatus(@PathVariable Long id) {
        try {
            vaccineService.deleteVaccine(id);
            return new ResponseEntity<>(new ApiResponse<>("Toggle trạng thái vaccine thành công"), HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(new ApiResponse<>(e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse<>("Lỗi server khi toggle trạng thái vaccine"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public class ApiResponse<T> {
        private boolean success;
        private T data;
        private String message;

        public ApiResponse(T data) {
            this.success = true;
            this.data = data;
            this.message = null;
        }

        public ApiResponse(String message) {
            this.success = false;
            this.data = null;
            this.message = message;
        }

        // Getters và setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
}

}