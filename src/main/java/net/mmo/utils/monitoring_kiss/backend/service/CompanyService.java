
package net.mmo.utils.monitoring_kiss.backend.service;

import net.mmo.utils.monitoring_kiss.backend.entity.Company;
import net.mmo.utils.monitoring_kiss.backend.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the service to access and store Companies
 */
@Service
public class CompanyService
{
	private CompanyRepository companyRepository;

	public CompanyService(CompanyRepository companyRepository) {
		this.companyRepository = companyRepository;
	}

	public List<Company> findAll() {
		return companyRepository.findAll();
	}
	
	public Map<String, Integer> getStats() {
		HashMap<String, Integer> stats = new HashMap<>();
		findAll().forEach(company -> stats.put(company.getName(), company.getEmployees().size()));
		return stats;
	}
}
