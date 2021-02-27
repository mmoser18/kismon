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
	                                      Double minDurationValue, Double maxDurationValue,
	                                      LocalDateTime timestampLowerBound, LocalDateTime timestampUpperBound) {
		if ((nameFilter == null || nameFilter.isEmpty()) &&
			typeFilter == null &&
			minDurationValue == null && maxDurationValue == null &&
			timestampLowerBound == null && timestampUpperBound == null) {
			return this.requestInfoRepository.findAll();
		} else {
			return this.requestInfoRepository.findByFilter(nameFilter, typeFilter,
			                                               minDurationValue, maxDurationValue,
			                                               timestampLowerBound, timestampUpperBound);
		}
	}

	public Double findLowestDurationWithFilter(String nameFilter, NodeType typeFilter) {
		return this.requestInfoRepository.findLowestDurationWithFilter(nameFilter, typeFilter);
	}

	public Double findHighestDurationWithFilter(String nameFilter, NodeType typeFilter) {
		return this.requestInfoRepository.findHighestDurationWithFilter(nameFilter, typeFilter);
	}

	public LocalDateTime findLowestTimestampWithFilter(String nameFilter, NodeType typeFilter) {
		return this.requestInfoRepository.findLowestTimestampWithFilter(nameFilter, typeFilter);
	}

	public LocalDateTime findHighestTimestampWithFilter(String nameFilter, NodeType typeFilter) {
		return this.requestInfoRepository.findHighestTimestampWithFilter(nameFilter, typeFilter);
	}

	public Long count() {
		return this.requestInfoRepository.count();
	}

	public Long countWithFilter(String nameFilter, NodeType typeFilter) {
		return this.requestInfoRepository.countWithFilter(nameFilter, typeFilter);
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
		log.trace("saved {}:", saved); //$NON-NLS-1$
		return saved;
	}

	public List<String> getNodeNames() {
		return this.requestInfoRepository.getNodeNames();
	}

	List<RequestInfo> getTimingDataForNode(String name,
	                                       LocalDateTime lowerBoundTimeStamp,
	                                       LocalDateTime upperBoundTimeStamp) {
		return  this.requestInfoRepository.findByFilter(name, null,
		                                                null, null,
		                                                lowerBoundTimeStamp, upperBoundTimeStamp);
	}
	List<RequestInfo> getTimingDataForNode(String name) {
		return getTimingDataForNode(name, null, null);
	}

//	public Map<String, Integer> getStats() {
//		HashMap<String, Integer> stats = new HashMap<>();
//		findAll().forEach(result -> stats.put(result.getName(), result.getEmployees().size()));
//		return stats;
//	}
}
