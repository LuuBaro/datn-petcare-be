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

    // Thêm mới employee
    public Employee createEmployee(Employee employee) {
        // Không kiểm tra employeeId, để JPA tự sinh
        return employeeRepository.save(employee);
    }

    // Cập nhật employee
    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy employee với ID: " + id));

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