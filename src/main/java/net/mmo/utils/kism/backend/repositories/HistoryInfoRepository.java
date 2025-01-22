/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.backend.repositories;

import java.time.LocalDateTime;
import java.util.List;

import net.mmo.utils.kism.entities.history.HistoryInfo;
import net.mmo.utils.kism.entities.nodes.VisibleNodeType;
import net.mmo.utils.kism.utils.OffsetLimitPageable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository class to access the database. Spring Boot makes this a painless process (well...):
 * All you need to do is define an interface that describes the entity nodeType and primary key
 * nodeType, and Spring Data will configure one for you.
 */
@SuppressWarnings("javadoc")
public interface HistoryInfoRepository extends JpaRepository<HistoryInfo, Long>, CustomHistoryInfoRepository
{
//	// using Java Persistence Query Language (JPQL) to phrase the query:

	final static String basicWhereClause =
		" where (:nameFilter is null or (concat('%', :nameFilter, '%') is null or lower(r.name) like lower(concat('%', :nameFilter, '%'))))" //$NON-NLS-1$
		+ " and (:typeFilter is null or r.nodeType = :typeFilter)" //$NON-NLS-1$
		;

	final static String durationWhereClause =
		  " and (:lowerBoundDuration is null or r.duration >= :lowerBoundDuration)" //$NON-NLS-1$
		+ " and (:upperBoundDuration is null or r.duration <= :upperBoundDuration)" //$NON-NLS-1$
		;
	final static String timestampWhereClause =
		  " and (:lowerBoundTimeStamp is null or r.timestamp >= :lowerBoundTimeStamp)" //$NON-NLS-1$
		+ " and (:upperBoundTimeStamp is null or r.timestamp <= :upperBoundTimeStamp)" //$NON-NLS-1$
		;
	final static String orderByIdClause =
					  " order by id asc"; //$NON-NLS-1$

	final static String orderByNameClause =
					  " order by name asc"; //$NON-NLS-1$

//	final static String limitAndOffset =
//		  " offset :offset limit :limit" //$NON-NLS-1$
//		;

	final static String fullWhereClause =
		  basicWhereClause
		+ durationWhereClause
		+ timestampWhereClause
		;

	/** unfortunately JPA doesn't support offset and limit, instead one needs to provide a {@link Pageable}
	 * - see {@link OffsetLimitPageable} for an implementation that translates between the two concepts
	 */
	@Query(value="select r from HistoryInfo r " + fullWhereClause + orderByIdClause)
	List<HistoryInfo> findByFilter(@Param("nameFilter") String nameFilter
	                              ,@Param("typeFilter") VisibleNodeType typeFilter
	                              ,@Param("lowerBoundDuration") Double minDurationValue
	                              ,@Param("upperBoundDuration") Double maxDurationValue
	                              ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
	                              ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
	                              ,Pageable pageable
	                              );

	@Query("select min(r.duration) from HistoryInfo r" + basicWhereClause)
	Double findLowestDurationWithFilter(@Param("nameFilter") String nameFilter
	                                   ,@Param("typeFilter") VisibleNodeType typeFilter
	                                   );

	@Query("select max(r.duration) from HistoryInfo r" + basicWhereClause)
	Double findHighestDurationWithFilter(@Param("nameFilter") String nameFilter
	                                    ,@Param("typeFilter") VisibleNodeType typeFilter
	                                    );

	@Query("select min(r.duration) from HistoryInfo r" + basicWhereClause + timestampWhereClause)
	Double findLowestDurationWithFilter(@Param("nameFilter") String nameFilter
	                                   ,@Param("typeFilter") VisibleNodeType typeFilter
	                                   ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
	                                   ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
	                                   );

	@Query("select max(r.duration) from HistoryInfo r" + basicWhereClause + timestampWhereClause)
	Double findHighestDurationWithFilter(@Param("nameFilter") String nameFilter
	                                    ,@Param("typeFilter") VisibleNodeType typeFilter
	                                    ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
	                                    ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
	                                    );

	@Query("select min(r.timestamp) from HistoryInfo r" + basicWhereClause)
	LocalDateTime findLowestTimestampWithFilter(@Param("nameFilter") String nameFilter
	                                           ,@Param("typeFilter") VisibleNodeType typeFilter
	                                           );

	@Query("select max(r.timestamp) from HistoryInfo r" + basicWhereClause)
	LocalDateTime findHighestTimestampWithFilter(@Param("nameFilter") String nameFilter
	                                            ,@Param("typeFilter") VisibleNodeType typeFilter
	                                            );

	@Query("select distinct r.name from HistoryInfo r" + orderByNameClause)
	List<String> getNodeNames();

	@Query("select distinct r.name from HistoryInfo r" + basicWhereClause + orderByNameClause)
	List<String> getNodeNames(@Param("nameFilter") String nameFilter
	                         ,@Param("typeFilter") VisibleNodeType typeFilter
	                         );

	@Query("select count(r.id) from HistoryInfo r" + basicWhereClause)
	Long countWithFilter(@Param("nameFilter") String nameFilter
                        ,@Param("typeFilter") VisibleNodeType typeFilter
                        );

	@Query("select count(r.id) from HistoryInfo r" + basicWhereClause + durationWhereClause + timestampWhereClause)
	Long countWithFilter(@Param("nameFilter") String nameFilter
	                    ,@Param("typeFilter") VisibleNodeType typeFilter
	                    ,@Param("lowerBoundDuration") Double minDurationValue
	                    ,@Param("upperBoundDuration") Double maxDurationValue
	                    ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
	                    ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
	                    );
}
