package org.example.petcarebe.service;

import org.example.petcarebe.model.Employee;
import org.example.petcarebe.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    // Lấy tất cả employees
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    // Lấy employee theo ID
    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    private void checkPhoneDuplicate(String phone, Long excludeId) {
        Optional<Employee> existingEmployee = employeeRepository.findByPhone(phone);
        if (existingEmployee.isPresent() && (excludeId == null || !existingEmployee.get().getEmployeeId().equals(excludeId))) {
            throw new RuntimeException("Số điện thoại " + phone + " đã được sử dụng.");
        }
    }

    // Thêm mới employee
    public Employee createEmployee(Employee employee) {
        checkPhoneDuplicate(employee.getPhone(), null);
        return employeeRepository.save(employee);
    }

    // Cập nhật employee
    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy employee với ID: " + id));

        checkPhoneDuplicate(employeeDetails.getPhone(), id); // Loại trừ chính employee đang cập nhật

        employee.setFullName(employeeDetails.getFullName());
        employee.setPhone(employeeDetails.getPhone());
        employee.setEmployeeType(employeeDetails.getEmployeeType());
        employee.setStatus(employeeDetails.getStatus());

        return employeeRepository.save(employee);
    }

    // Xóa employee (chuyển status thành "inactive" thay vì xóa vật lý)
    public Employee deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy employee với ID: " + id));
        employee.setStatus("inactive");
        return employeeRepository.save(employee);
    }
}