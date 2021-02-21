/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17 Feb 2021
 */

package net.mmo.utils.kism.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.repositories.RequestInfoRepository;
import net.mmo.utils.kism.entities.nodes.NodeType;
import net.mmo.utils.kism.entities.results.RequestInfo;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RequestInfoService
{
	private RequestInfoRepository requestInfoRepository;

	public RequestInfoService(RequestInfoRepository requestInfoRepository) {
		log.debug("RequestInfoService c'tor"); //$NON-NLS-1$
		this.requestInfoRepository = requestInfoRepository;
	}

	public List<RequestInfo> findAll() {
		return this.requestInfoRepository.findAll();
	}

	public List<RequestInfo> findByFilter(String nameFilter, NodeType typeFilter,
	                                      Long durationLowerBound, Long durationUpperBound,
	                                      LocalDateTime timestampLowerBound, LocalDateTime timestampUpperBound) {
		if ((nameFilter == null || nameFilter.isEmpty()) &&
			typeFilter == null &&
			durationLowerBound == null && durationUpperBound == null &&
			timestampLowerBound == null && timestampUpperBound == null) {
			return this.requestInfoRepository.findAll();
		} else {
			return this.requestInfoRepository.findByFilter(nameFilter, typeFilter,
			                                               durationLowerBound, durationUpperBound,
			                                               timestampLowerBound, timestampUpperBound);
		}
	}

	public Long findLowestDurationWithFilter(String nameFilter, NodeType typeFilter) {
		return this.requestInfoRepository.findLowestDurationWithFilter(nameFilter, typeFilter);
	}

	public Long findHighestDurationWithFilter(String nameFilter, NodeType typeFilter) {
		return this.requestInfoRepository.findHighestDurationWithFilter(nameFilter, typeFilter);
	}

	public Long count() {
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
