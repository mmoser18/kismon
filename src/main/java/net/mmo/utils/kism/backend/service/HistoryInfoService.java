/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17 Feb 2021
 */

package net.mmo.utils.kism.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.repositories.HistoryInfoRepository;
import net.mmo.utils.kism.entities.history.HistoryInfo;
import net.mmo.utils.kism.entities.nodes.NodeType;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HistoryInfoService
{
	protected HistoryInfoRepository historyInfoRepository;

	public boolean dbIsAvailable = false;
//
//	@Autowired
//	private EntityManager entityManager;
//
	public HistoryInfoService(HistoryInfoRepository historyInfoRepository) {
		log.debug("HistoryInfoService c'tor"); //$NON-NLS-1$
		try {
			this.historyInfoRepository = historyInfoRepository;

//			// This finally revealed how to do this:
//			// https://stackoverflow.com/questions/22116005/how-to-create-jpa-repository-dynamically-inside-a-class
//			RepositoryFactorySupport factory = new JpaRepositoryFactory(this.entityManager);
//			this.historyInfoRepository = (HistoryInfoRepository)factory.getRepository(CustomHistoryInfoRepository.class);
			this.dbIsAvailable = true;

		} catch (Exception ex) {
			log.error("Failed to create HistoryInfoRepository - is the service running and accessible?", ex); //$NON-NLS-1$
		}
	}

	public List<HistoryInfo> findAll() {
		return this.historyInfoRepository.findAll();
	}

	public List<HistoryInfo> findByFilter(String nameFilter, NodeType typeFilter,
	                                      Double minDurationValue, Double maxDurationValue,
	                                      LocalDateTime timestampLowerBound, LocalDateTime timestampUpperBound) {
		if ((nameFilter == null || nameFilter.isEmpty()) &&
			typeFilter == null &&
			minDurationValue == null && maxDurationValue == null &&
			timestampLowerBound == null && timestampUpperBound == null) {
			return this.historyInfoRepository.findAll();
		} else {
			return this.historyInfoRepository.findByFilter(nameFilter, typeFilter,
			                                               minDurationValue, maxDurationValue,
			                                               timestampLowerBound, timestampUpperBound);
		}
	}

	public Double findLowestDurationWithFilter(String nameFilter, NodeType typeFilter) {
		return this.historyInfoRepository.findLowestDurationWithFilter(nameFilter, typeFilter);
	}

	public Double findHighestDurationWithFilter(String nameFilter, NodeType typeFilter) {
		return this.historyInfoRepository.findHighestDurationWithFilter(nameFilter, typeFilter);
	}

	public LocalDateTime findLowestTimestampWithFilter(String nameFilter, NodeType typeFilter) {
		return this.historyInfoRepository.findLowestTimestampWithFilter(nameFilter, typeFilter);
	}

	public LocalDateTime findHighestTimestampWithFilter(String nameFilter, NodeType typeFilter) {
		return this.historyInfoRepository.findHighestTimestampWithFilter(nameFilter, typeFilter);
	}

	public Long count() {
		return this.historyInfoRepository.count();
	}

	public Long countWithFilter(String nameFilter, NodeType typeFilter) {
		return this.historyInfoRepository.countWithFilter(nameFilter, typeFilter);
	}


	public void delete(HistoryInfo historyInfo) {
		this.historyInfoRepository.delete(historyInfo);
	}

	public HistoryInfo save(HistoryInfo historyInfo) {
		log.debug("saving {}:", historyInfo); //$NON-NLS-1$
		if (historyInfo == null) {
			log.error("historyInfo is null"); //$NON-NLS-1$
			return null;
		}
		HistoryInfo saved = this.historyInfoRepository.save(historyInfo);
		log.trace("saved {}:", saved); //$NON-NLS-1$
		return saved;
	}

	public List<String> getNodeNames() {
		return this.historyInfoRepository.getNodeNames();
	}

	public List<String> getNodeNames(String nameFilter, NodeType typeFilter) {
		return this.historyInfoRepository.getNodeNames(nameFilter, typeFilter);
	}


	List<HistoryInfo> getTimingDataForNode(String name,
	                                       LocalDateTime lowerBoundTimeStamp,
	                                       LocalDateTime upperBoundTimeStamp) {
		return  this.historyInfoRepository.findByFilter(name, null,
		                                                null, null,
		                                                lowerBoundTimeStamp, upperBoundTimeStamp);
	}
	List<HistoryInfo> getTimingDataForNode(String name) {
		return getTimingDataForNode(name, null, null);
	}

//	public Map<String, Integer> getStats() {
//		HashMap<String, Integer> stats = new HashMap<>();
//		findAll().forEach(result -> stats.put(result.getName(), result.getEmployees().size()));
//		return stats;
//	}
}
