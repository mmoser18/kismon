/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17 Feb 2021
 */

package net.mmo.utils.kism.backend.service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.repositories.ResultRepository;
import net.mmo.utils.kism.entities.results.RequestResults;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResultService
{
	private ResultRepository resultRepository;

	public ResultService(ResultRepository resultRepository) {
		this.resultRepository = resultRepository;
	}

	public List<RequestResults> findAll() {
		return this.resultRepository.findAll();
	}

	public List<RequestResults> findAll(String stringFilter) {
		if (stringFilter == null || stringFilter.isEmpty()) {
			return this.resultRepository.findAll();
		} else {
			return this.resultRepository.findByName(stringFilter);
		}
	}

	public long count() {
		return this.resultRepository.count();
	}

	public void delete(RequestResults contact) {
		this.resultRepository.delete(contact);
	}

	public void save(RequestResults contact) {
		if (contact == null) {
			log.error("Contact is null. Are you sure you have connected your form to the application?"); //$NON-NLS-1$
			return;
		}
		this.resultRepository.save(contact);
	}

//	public Map<String, Integer> getStats() {
//		HashMap<String, Integer> stats = new HashMap<>();
//		findAll().forEach(result -> stats.put(result.getName(), result.getEmployees().size()));
//		return stats;
//	}
}
