/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17 Feb 2021
 */

package net.mmo.utils.kism.backend.repositories;

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
	// using Java Persistence Query Language (JPQL) to phrase the query:
	@Query("select r from RequestInfo r "
	      + "where (concat('%', :nameFilter, '%') is null or lower(r.name) like lower(concat('%', :nameFilter, '%')))"
	      + "  and (concat('%', :typeFilter, '%') is null or r.nodeType = :typeFilter)"
//	      + "  and (concat('%', :typeFilter, '%') is null or lower(r.nodeType) like lower(concat('%', :typeFilter, '%')))"
//	      + "  and (concat('%', :lowerBoundDuration, '%') is null or r.duration >= concat('%', :lowerBoundDuration, '%')))"
//	      + "  and (concat('%', :upperBoundDuration, '%') is null or r.duration <= concat('%', :upperBoundDuration, '%')))"
//	      + "  and (concat('%', :lowerBoundTimeStamp, '%') is null or r.timestamp >= concat('%', :lowerBoundTimeStamp, '%')))"
//	      + "  and (concat('%', :upperBoundTimeStamp, '%') is null or r.timestamp <= concat('%', :upperBoundTimeStamp, '%')))"
	)
	List<RequestInfo> findByFilter(@Param("nameFilter") String nameFilter
	                              ,@Param("typeFilter") NodeType typeFilter
//	                              ,@Param("lowerBoundDuration") long lowerBoundDuration
//	                              ,@Param("upperBoundDuration") long upperBoundDuration
//	                              ,@Param("lowerBoundTimeStamp") LocalDateTime lowerBoundTimeStamp
//	                              ,@Param("upperBoundTimeStamp") LocalDateTime upperBoundTimeStamp
	                              );

//	@Query("select r from RequestInfo r"
//	      + " where (concat('%', :lowerBound, '%') is null or r.duration >= concat('%', :lowerBound, '%'))"
//	      + "   and (concat('%', :upperBound, '%') is null or r.duration <= concat('%', :upperBound, '%'))"
//	)
//	List<RequestInfo> findByDuration(@Param("lowerBound") long lowerBound, @Param("upperBound") long upperBound);
//
//	@Query("select r from RequestInfo r"
//	      + " where (concat('%', :lowerBound, '%') is null or r.timestamp >= concat('%', :lowerBound, '%'))"
//	      + "   and (concat('%', :upperBound, '%') is null or r.timestamp <= concat('%', :upperBound, '%'))"
//	)
//	List<RequestInfo> findByTimestamp(@Param("lowerBound") LocalDateTime lowerBound, @Param("upperBound") LocalDateTime upperBound);
}
