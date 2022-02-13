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
import net.mmo.utils.kism.entities.nodes.VisibleNodeType;
import net.mmo.utils.kism.utils.OffsetLimitPageable;
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

	public List<HistoryInfo> findByFilter(String nameFilter, VisibleNodeType typeFilter,
	                                      Double minDurationValue, Double maxDurationValue,
	                                      LocalDateTime timestampLowerBound, LocalDateTime timestampUpperBound,
	                                      int offset, int limit) {
		if ((nameFilter == null || nameFilter.isEmpty()) &&
			typeFilter == null &&
			minDurationValue == null && maxDurationValue == null &&
			timestampLowerBound == null && timestampUpperBound == null &&
			offset <= 0 && limit <= 0) {
			return this.historyInfoRepository.findAll();
		} else {
			return this.historyInfoRepository.findByFilter(nameFilter, typeFilter
			                                              , minDurationValue, maxDurationValue
			                                              , timestampLowerBound, timestampUpperBound
			                                              , OffsetLimitPageable.of(offset, limit)
			                                              );
		}
	}

	public Double findLowestDurationWithFilter(String nameFilter, VisibleNodeType typeFilter) {
		return this.historyInfoRepository.findLowestDurationWithFilter(nameFilter, typeFilter);
	}

	public Double findHighestDurationWithFilter(String nameFilter, VisibleNodeType typeFilter) {
		return this.historyInfoRepository.findHighestDurationWithFilter(nameFilter, typeFilter);
	}

	public LocalDateTime findLowestTimestampWithFilter(String nameFilter, VisibleNodeType typeFilter) {
		return this.historyInfoRepository.findLowestTimestampWithFilter(nameFilter, typeFilter);
	}

	public LocalDateTime findHighestTimestampWithFilter(String nameFilter, VisibleNodeType typeFilter) {
		return this.historyInfoRepository.findHighestTimestampWithFilter(nameFilter, typeFilter);
	}

	public Long count() {
		return this.historyInfoRepository.count();
	}

	public Long countWithFilter(String nameFilter, VisibleNodeType typeFilter,
	                            Double minDurationValue, Double maxDurationValue,
	                            LocalDateTime timestampLowerBound, LocalDateTime timestampUpperBound) {
		return this.historyInfoRepository.countWithFilter(nameFilter, typeFilter,
		                                                  minDurationValue, maxDurationValue,
		                                                  timestampLowerBound, timestampUpperBound);
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

	public List<String> getNodeNames(String nameFilter, VisibleNodeType typeFilter) {
		return this.historyInfoRepository.getNodeNames(nameFilter, typeFilter);
	}


	public List<HistoryInfo> getTimingDataForNode(String name,
	                                       LocalDateTime lowerBoundTimeStamp,
	                                       LocalDateTime upperBoundTimeStamp,
	                                       int offset, int limit) {
		return this.historyInfoRepository.findByFilter(name, null
		                                              , null, null
		                                              , lowerBoundTimeStamp, upperBoundTimeStamp
		                                              , OffsetLimitPageable.of(offset, limit)
		                                              );
	}
	public List<HistoryInfo> getTimingDataForNode(String name,int offset, int limit) {
		return getTimingDataForNode(name, null, null, offset, limit);
	}

//	public Map<String, Integer> getStats() {
//		HashMap<String, Integer> stats = new HashMap<>();
//		findAll().forEach(result -> stats.put(result.getName(), result.getEmployees().size()));
//		return stats;
//	}
}
