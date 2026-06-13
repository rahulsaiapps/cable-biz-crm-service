package com.cablepulse.repository;

import com.cablepulse.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    Optional<Employee> findByEmailIgnoreCaseAndWorkspaceId(String email, String workspaceId);

    List<Employee> findByWorkspaceId(String workspaceId);

    boolean existsByWorkspaceIdAndRole(String workspaceId, com.cablepulse.model.EmployeeRole role);
}
