/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17 Feb 2021
 */

package net.mmo.utils.kism.backend.repositories;

import java.time.LocalDateTime;
import java.util.List;

import net.mmo.utils.kism.entities.nodes.NodeType;
import net.mmo.utils.kism.entities.results.RequestInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository class to access the database. Spring Boot makes this a painless process:
 * All you need to do is define an interface that describes the entity nodeType and primary key nodeType,
 * and Spring Data will configure one for you.
 */
public interface RequestInfoRepository extends JpaRepository<RequestInfo, Long>
{
	final static String basicWhereClause =
		"where (concat('%', :nameFilter, '%') is null or lower(r.name) like lower(concat('%', :nameFilter, '%'))) " //$NON-NLS-1$
		+ "and (:typeFilter is null or r.nodeType = :typeFilter) " //$NON-NLS-1$
		;

	final static String fullWhereClause =
					basicWhereClause
					+ "and (:lowerBoundDuration is null or r.duration >= :lowerBoundDuration) " //$NON-NLS-1$
					+ "and (:upperBoundDuration is null or r.duration <= :upperBoundDuration) " //$NON-NLS-1$
					+ "and (:lowerBoundTimeStamp is null or r.timestamp >= :lowerBoundTimeStamp) " //$NON-NLS-1$
					+ "and (:upperBoundTimeStamp is null or r.timestamp <= :upperBoundTimeStamp) " //$NON-NLS-1$
					;

	// using Java Persistence Query Language (JPQL) to phrase the query:
	@Query("select r from RequestInfo r " + fullWhereClause)
	List<RequestInfo> findByFilter(@Param("nameFilter") String nameFilter
	                              ,@Param("typeFilter") NodeType typeFilter
	                              ,@Param("lowerBoundDuration") Long lowerBoundDuration
	                              ,@Param("upperBoundDuration") Long upperBoundDuration
	                              ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
	                              ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
	                              );


	@Query("select min(r.duration) from RequestInfo r " + basicWhereClause)
	Long findLowestDurationWithFilter(@Param("nameFilter") String nameFilter
	                                 ,@Param("typeFilter") NodeType typeFilter
	                                 );

	@Query("select max(r.duration) from RequestInfo r " + basicWhereClause)
	Long findHighestDurationWithFilter(@Param("nameFilter") String nameFilter
	                                  ,@Param("typeFilter") NodeType typeFilter
	                                  );

//	@Query("select min(r.timestamp) from RequestInfo r " + whereClause)
//	List<RequestInfo> findLowestTimestampWithFilter(@Param("nameFilter") String nameFilter
//	                                               ,@Param("typeFilter") NodeType typeFilter
//	                                               ,@Param("lowerBoundDuration") long lowerBoundDuration
//	                                               ,@Param("upperBoundDuration") long upperBoundDuration
//	                                               ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
//	                                               ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
//	                                               );
//
//	@Query("select max(r.timestamp) from RequestInfo r " + whereClause)
//	List<RequestInfo> findHighstTimestampWithFilter(@Param("nameFilter") String nameFilter
//	                                               ,@Param("typeFilter") NodeType typeFilter
//	                                               ,@Param("lowerBoundDuration") long lowerBoundDuration
//	                                               ,@Param("upperBoundDuration") long upperBoundDuration
//	                                               ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
//	                                               ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
//	                                               );
}
