/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17 Feb 2021
 */

package net.mmo.utils.kism.backend.repositories;

import java.time.LocalDateTime;
import java.util.List;

import net.mmo.utils.kism.entities.history.HistoryInfo;
import net.mmo.utils.kism.entities.nodes.NodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository class to access the database. Spring Boot makes this a painless process:
 * All you need to do is define an interface that describes the entity nodeType and primary key
 * nodeType, and Spring Data will configure one for you.
 */
public interface HistoryInfoRepository extends JpaRepository<HistoryInfo, Long>
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

//	final static String limitAndOffset =
//					"limit :limit offset :offset " //$NON-NLS-1$
//					;

	final static String fullWhereClause =
					  basicWhereClause
					+ durationWhereClause
					+ timestampWhereClause
					;

	@Query("select r from HistoryInfo r " + fullWhereClause)
	List<HistoryInfo> findByFilter(@Param("nameFilter") String nameFilter
	                              ,@Param("typeFilter") NodeType typeFilter
	                              ,@Param("lowerBoundDuration") Double minDurationValue
	                              ,@Param("upperBoundDuration") Double maxDurationValue
	                              ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
	                              ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
	                              );

//	@Query("select r from HistoryInfo r " + fullWhereClause + limitAndOffset)
//	List<HistoryInfo> findByFilter(@Param("nameFilter") String nameFilter
//	                              ,@Param("typeFilter") NodeType typeFilter
//	                              ,@Param("lowerBoundDuration") Double minDurationValue
//	                              ,@Param("upperBoundDuration") Double maxDurationValue
//	                              ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
//	                              ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
//	                              ,@Param("offset") int offset
//	                              ,@Param("limit") int limit
//	                              );

	@Query("select min(r.duration) from HistoryInfo r" + basicWhereClause)
	Double findLowestDurationWithFilter(@Param("nameFilter") String nameFilter
	                                   ,@Param("typeFilter") NodeType typeFilter
	                                   );

	@Query("select max(r.duration) from HistoryInfo r" + basicWhereClause)
	Double findHighestDurationWithFilter(@Param("nameFilter") String nameFilter
	                                    ,@Param("typeFilter") NodeType typeFilter
	                                    );

	@Query("select min(r.duration) from HistoryInfo r" + basicWhereClause + timestampWhereClause)
	Double findLowestDurationWithFilter(@Param("nameFilter") String nameFilter
	                                   ,@Param("typeFilter") NodeType typeFilter
	                                   ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
	                                   ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
	                                   );

	@Query("select max(r.duration) from HistoryInfo r" + basicWhereClause + timestampWhereClause)
	Double findHighestDurationWithFilter(@Param("nameFilter") String nameFilter
	                                    ,@Param("typeFilter") NodeType typeFilter
	                                    ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
	                                    ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
	                                    );

	@Query("select min(r.timestamp) from HistoryInfo r" + basicWhereClause)
	LocalDateTime findLowestTimestampWithFilter(@Param("nameFilter") String nameFilter
	                                           ,@Param("typeFilter") NodeType typeFilter
	                                           );

	@Query("select max(r.timestamp) from HistoryInfo r" + basicWhereClause)
	LocalDateTime findHighestTimestampWithFilter(@Param("nameFilter") String nameFilter
	                                            ,@Param("typeFilter") NodeType typeFilter
	                                            );

	@Query("select distinct r.name from HistoryInfo r")
	List<String> getNodeNames();

	@Query("select distinct r.name from HistoryInfo r" + basicWhereClause)
	List<String> getNodeNames(@Param("nameFilter") String nameFilter
	                         ,@Param("typeFilter") NodeType typeFilter
	                         );

	@Query("select count(r.id) from HistoryInfo r" + basicWhereClause)
	Long countWithFilter(@Param("nameFilter") String nameFilter
                        ,@Param("typeFilter") NodeType typeFilter
                        );

	@Query("select count(r.id) from HistoryInfo r" + basicWhereClause + timestampWhereClause)
	Long countWithFilter(@Param("nameFilter") String nameFilter
	                    ,@Param("typeFilter") NodeType typeFilter
	                    ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
	                    ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
	                    );
}
