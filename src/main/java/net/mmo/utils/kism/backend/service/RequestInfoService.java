/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17 Feb 2021
 */

package net.mmo.utils.kism.backend.service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.repositories.RequestInfoRepository;
import net.mmo.utils.kism.entities.results.RequestInfo;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RequestInfoService
{
	private RequestInfoRepository requestInfoRepository;

	public RequestInfoService(RequestInfoRepository requestInfoRepository) {
		this.requestInfoRepository = requestInfoRepository;
	}

	public List<RequestInfo> findAll() {
		return this.requestInfoRepository.findAll();
	}

	public List<RequestInfo> findAll(String stringFilter) {
		if (stringFilter == null || stringFilter.isEmpty()) {
			return this.requestInfoRepository.findAll();
		} else {
			return this.requestInfoRepository.findByName(stringFilter);
		}
	}

	public long count() {
		return this.requestInfoRepository.count();
	}

	public void delete(RequestInfo requestInfo) {
		this.requestInfoRepository.delete(requestInfo);
	}

	public RequestInfo save(RequestInfo requestInfo) {
		log.debug("saving {}:", requestInfo); //$NON-NLS-1$
		if (requestInfo == null) {
			log.error("requestInfo is null"); //$NON-NLS-1$
			return null;
		}
		RequestInfo saved = this.requestInfoRepository.save(requestInfo);
		log.info("saved {}:", saved); //$NON-NLS-1$
		return saved;
	}

//	public Map<String, Integer> getStats() {
//		HashMap<String, Integer> stats = new HashMap<>();
//		findAll().forEach(result -> stats.put(result.getName(), result.getEmployees().size()));
//		return stats;
//	}
}
