package com.cablepulse.repository;

import com.cablepulse.model.OperatorCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperatorCompanyRepository extends JpaRepository<OperatorCompany, Long> {
}
